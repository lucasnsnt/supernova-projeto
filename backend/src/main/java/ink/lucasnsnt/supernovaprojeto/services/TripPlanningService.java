package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.config.DailyTransportProperties;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.TripResponse;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.models.*;
import ink.lucasnsnt.supernovaprojeto.models.enums.*;
import ink.lucasnsnt.supernovaprojeto.repositories.DailyConfirmationRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.TripParticipantRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.TripRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.VehicleRepository;
import ink.lucasnsnt.supernovaprojeto.services.routing.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripPlanningService {

    private final DailyConfirmationRepository confirmationRepository;
    private final TripParticipantRepository participantRepository;
    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final RoutePlanningGateway routePlanningGateway;
    private final InAppNotificationService notificationService;
    private final DailyTransportProperties properties;
    private final Clock clock;
    private final GeocodingService geocodingService;
    private final DriverService driverService;
    private final ink.lucasnsnt.supernovaprojeto.repositories.DriverRepository driverRepository;
    private final RouteEligibilityService eligibility;

    @Transactional
    public int planReadyConfirmations() {
        LocalDateTime now = LocalDateTime.now(clock);
        Map<PlanningKey, List<DailyConfirmation>> groups = confirmationRepository
                .findAllByStatusAndResponseDeadlineLessThanEqualOrderByResponseDeadline(
                        DailyConfirmationStatus.YES, now)
                .stream()
                .filter(confirmation -> confirmation.getRecurringRoute() != null)
                .filter(eligibility::eligible)
                .filter(confirmation -> !participantRepository.existsByConfirmationId(confirmation.getId()))
                .sorted(Comparator.comparing(c -> c.getDriver().getId()))
                .collect(Collectors.groupingBy(
                        confirmation -> new PlanningKey(
                                confirmation.getDriver().getId(), confirmation.getServiceDate(),
                                confirmation.getDirection(), confirmation.getRecurringRoute() == null ? null : confirmation.getScheduledTime(),
                                confirmation.getRecurringRoute() == null ? null : confirmation.getRecurringRoute().getId()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        int tripsCreated = 0;
        for (List<DailyConfirmation> confirmations : groups.values()) {
            DailyConfirmation first = confirmations.getFirst();
            driverRepository.lockById(first.getDriver().getId());
            if (tripRepository.findByRecurringRouteIdAndServiceDateAndDirection(
                    first.getRecurringRoute().getId(), first.getServiceDate(), first.getDirection()).isPresent()) continue;
            List<DailyConfirmation> eligible = confirmed(first.getRecurringRoute().getId(), first.getServiceDate(), first.getDirection());
            if (eligible.isEmpty()) continue;
            Trip trip = build(first.getRecurringRoute(), first.getServiceDate(), first.getDirection(),
                    first.getPreliminaryDepartureAt(), first.getPreliminaryDepartureAt(), eligible);
            tripRepository.save(trip);
            if (trip.getStatus() == TripStatus.PLANNED) notifyTripPlanned(trip);
            else notificationService.create(first.getDriver().getId(), NotificationType.PLANNING_NEEDS_ATTENTION,
                    "Planejamento precisa de atenção", trip.getPlanningIssue(), trip, null);
            tripsCreated++;
        }
        return tripsCreated;
    }

    @Transactional
    public TripResponse replan(Long driverId, Long tripId) {
        driverService.requireApproved(driverId);
        Trip trip = tripRepository.findByIdAndDriverId(tripId, driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem", tripId));
        if (trip.getStatus() != TripStatus.NEEDS_ATTENTION) {
            throw new BusinessRuleException(
                    "Somente uma viagem que precisa de atenção pode ser recalculada");
        }
        driverRepository.lockById(driverId);
        if (trip.getRecurringRoute() == null) throw new BusinessRuleException("Viagem antiga sem rota do motorista");
        List<DailyConfirmation> confirmations = confirmed(trip.getRecurringRoute().getId(), trip.getServiceDate(), trip.getDirection());
        Trip planned = build(trip.getRecurringRoute(), trip.getServiceDate(), trip.getDirection(),
                trip.getPlannedDepartureAt(), trip.getPlannedDepartureAt(), confirmations);
        merge(trip, planned);
        if (trip.getStatus() == TripStatus.PLANNED) notifyTripPlanned(trip);
        return TripResponse.from(trip);
    }

    List<DailyConfirmation> confirmed(Long routeId, LocalDate date, Direction direction) {
        return confirmationRepository.findAllByRecurringRouteIdAndServiceDateAndDirection(routeId, date, direction)
                .stream().filter(item -> item.getStatus() == DailyConfirmationStatus.YES)
                .filter(eligibility::eligible)
                .filter(item -> !participantRepository.existsByConfirmationId(item.getId())
                        || tripRepository.findByRecurringRouteIdAndServiceDateAndDirection(routeId, date, direction)
                        .map(trip -> trip.getParticipants().stream().anyMatch(p -> p.getConfirmation().getId().equals(item.getId()))).orElse(false))
                .peek(item -> {
                    // V8 did not invent historical deadlines. An unstarted occurrence
                    // may now take an explicit snapshot of the student's current class.
                    if (item.getAcademicTime() == null) item.getStudent().getSchedules().stream()
                            .filter(schedule -> schedule.getDayOfWeek() == date.getDayOfWeek() && schedule.getDirection() == direction)
                            .findFirst().ifPresent(schedule -> item.setAcademicTime(schedule.getTime()));
                }).toList();
    }

    Trip build(RecurringRoute recurring, LocalDate date, Direction direction, LocalDateTime scheduled,
            LocalDateTime departure, List<DailyConfirmation> confirmations) {
        RoutePlanningResult route = confirmations.isEmpty()
                ? RoutePlanningResult.unavailable("Nenhum aluno confirmou presença nesta saída")
                : optimize(confirmations, recurring.getVehicle(), departure);
        Trip trip = Trip.builder().driver(recurring.getDriver()).recurringRoute(recurring)
                .vehicle(recurring.getVehicle()).serviceDate(date).direction(direction)
                .status(route.feasible() ? TripStatus.PLANNED : TripStatus.NEEDS_ATTENTION)
                .plannedDepartureAt(scheduled).driverDepartureAt(departure).planningIssue(route.issue())
                .routeProvider(route.provider()).routeReference(route.reference()).encodedPolyline(route.encodedPolyline())
                .routeCalculatedAt(route.feasible() ? LocalDateTime.now(clock) : null)
                .createdAt(LocalDateTime.now(clock)).build();
        Map<Long, RouteStopPlan> stops = route.stops().stream()
                .collect(Collectors.toMap(RouteStopPlan::confirmationId, stop -> stop));
        int order = 1;
        for (DailyConfirmation confirmation : confirmations) {
            RouteStopPlan stop = stops.get(confirmation.getId());
            trip.addParticipant(TripParticipant.builder().student(confirmation.getStudent()).confirmation(confirmation)
                    .pickupOrder(stop == null ? order : stop.pickupOrder())
                    .dropoffOrder(stop == null ? order + confirmations.size() : stop.dropoffOrder())
                    .estimatedPickupAt(stop == null ? null : stop.estimatedPickupAt())
                    .estimatedDropoffAt(stop == null ? null : stop.estimatedDropoffAt()).build());
            order++;
        }
        return trip;
    }

    void merge(Trip existing, Trip planned) {
        existing.setVehicle(planned.getVehicle());
        existing.setPlannedDepartureAt(planned.getPlannedDepartureAt());
        existing.setStatus(planned.getStatus());
        existing.setDriverDepartureAt(planned.getDriverDepartureAt());
        existing.setPlanningIssue(planned.getPlanningIssue());
        existing.setRouteProvider(planned.getRouteProvider());
        existing.setRouteReference(planned.getRouteReference());
        existing.setEncodedPolyline(planned.getEncodedPolyline());
        existing.setRouteCalculatedAt(planned.getRouteCalculatedAt());
        Set<Long> ids = planned.getParticipants().stream().map(p -> p.getConfirmation().getId()).collect(Collectors.toSet());
        existing.getParticipants().removeIf(p -> !ids.contains(p.getConfirmation().getId()));
        for (TripParticipant next : planned.getParticipants()) {
            TripParticipant current = existing.getParticipants().stream()
                    .filter(p -> p.getConfirmation().getId().equals(next.getConfirmation().getId())).findFirst().orElse(null);
            if (current == null) existing.addParticipant(next);
            else {
                current.setPickupOrder(next.getPickupOrder()); current.setDropoffOrder(next.getDropoffOrder());
                current.setEstimatedPickupAt(next.getEstimatedPickupAt()); current.setEstimatedDropoffAt(next.getEstimatedDropoffAt());
            }
        }
    }

    private RoutePlanningResult optimize(List<DailyConfirmation> confirmations, Vehicle vehicle, LocalDateTime departure) {
        if (vehicle == null || confirmations.size() > vehicle.getPassengerCapacity()) return RoutePlanningResult.unavailable("O veículo da rota não possui capacidade para todos os confirmados");
        if (confirmations.stream().anyMatch(c -> c.getAcademicTime() == null)) return RoutePlanningResult.unavailable("Há confirmações antigas sem horário de aula; atualize os horários antes de iniciar");
        DailyConfirmation first = confirmations.getFirst();
        Address base = first.getDriver().getOperationalAddress() == null
                ? first.getDriver().getUser().getAddress()
                : first.getDriver().getOperationalAddress();
        try {
            geocodingService.resolve(base);
            for (DailyConfirmation confirmation : confirmations) {
                geocodingService.resolve(confirmation.getStudent().getUser().getAddress());
                geocodingService.resolve(confirmation.getStudent().getInstitution().getAddress());
            }
        } catch (BusinessRuleException exception) {
            return RoutePlanningResult.unavailable(exception.getMessage());
        }
        List<RoutePassenger> passengers = confirmations.stream().map(this::passenger).toList();
        if (point(base) == null || passengers.stream().anyMatch(this::hasMissingPoint)) {
            return RoutePlanningResult.unavailable("Existem endereços sem coordenadas válidas");
        }
        return routePlanningGateway.optimize(new RoutePlanningRequest(
                first.getDriver().getId(),
                first.getServiceDate(),
                first.getDirection(),
                vehicle.getPassengerCapacity(),
                point(base),
                first.getDirection() == Direction.VOLTA ? point(base) : null,
                passengers, departure));
    }

    private RoutePassenger passenger(DailyConfirmation confirmation) {
        Address home = confirmation.getStudent().getUser().getAddress();
        Address institution = confirmation.getStudent().getInstitution().getAddress();
        LocalDateTime scheduled = confirmation.getServiceDate().atTime(confirmation.getAcademicTime());
        if (confirmation.getDirection() == Direction.IDA) {
            return new RoutePassenger(confirmation.getId(), confirmation.getStudent().getId(),
                    point(home), point(institution), null, null, scheduled);
        }
        return new RoutePassenger(confirmation.getId(), confirmation.getStudent().getId(),
                point(institution), point(home), scheduled,
                scheduled.plus(properties.getMaximumReturnWait()), null);
    }

    private RoutePoint point(Address address) {
        if (address == null || address.getLatitude() == null || address.getLongitude() == null) {
            return null;
        }
        return new RoutePoint(address.getLatitude(), address.getLongitude());
    }

    private boolean hasMissingPoint(RoutePassenger passenger) {
        return passenger.pickup() == null || passenger.dropoff() == null
                || !passenger.pickup().isComplete() || !passenger.dropoff().isComplete();
    }

    private void notifyTripPlanned(Trip trip) {
        trip.getParticipants().forEach(participant -> notificationService.create(
                participant.getStudent().getId(),
                NotificationType.TRIP_PLANNED,
                "Viagem planejada",
                "Sua viagem está prevista para " + trip.getPlannedDepartureAt().toLocalTime(),
                trip,
                participant.getConfirmation()));
    }

    private record PlanningKey(Long driverId, LocalDate date, Direction direction,
                               java.time.LocalTime scheduledTime, Long recurringRouteId) {
    }
}
