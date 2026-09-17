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

    @Transactional
    public int planReadyConfirmations() {
        LocalDateTime now = LocalDateTime.now(clock);
        Map<PlanningKey, List<DailyConfirmation>> groups = confirmationRepository
                .findAllByStatusAndResponseDeadlineLessThanEqualOrderByResponseDeadline(
                        DailyConfirmationStatus.YES, now)
                .stream()
                .filter(confirmation -> confirmation.getDriver().getStatus() == DriverStatus.APPROVED)
                .filter(confirmation -> !participantRepository.existsByConfirmationId(confirmation.getId()))
                .collect(Collectors.groupingBy(
                        confirmation -> new PlanningKey(
                                confirmation.getDriver().getId(), confirmation.getServiceDate(),
                                confirmation.getDirection(), confirmation.getRecurringRoute() == null ? null : confirmation.getScheduledTime(),
                                confirmation.getRecurringRoute() == null ? null : confirmation.getRecurringRoute().getId()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        int tripsCreated = 0;
        for (List<DailyConfirmation> confirmations : groups.values()) {
            Vehicle vehicle = vehicleRepository
                    .findFirstByDriverIdAndDefaultVehicleTrue(confirmations.getFirst().getDriver().getId())
                    .orElse(null);
            for (List<DailyConfirmation> batch : partition(confirmations, vehicle)) {
                createTrip(batch, vehicle, now);
                tripsCreated++;
            }
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
        Vehicle vehicle = trip.getVehicle() == null
                ? vehicleRepository.findFirstByDriverIdAndDefaultVehicleTrue(driverId).orElse(null)
                : trip.getVehicle();
        List<DailyConfirmation> confirmations = trip.getParticipants().stream()
                .map(TripParticipant::getConfirmation)
                .toList();
        RoutePlanningResult route = vehicle == null
                ? RoutePlanningResult.unavailable("O motorista não possui um veículo padrão")
                : optimize(confirmations, vehicle);
        applyReplanning(trip, vehicle, route, LocalDateTime.now(clock));
        if (route.feasible()) {
            notifyTripPlanned(trip);
        }
        return TripResponse.from(trip);
    }

    private List<List<DailyConfirmation>> partition(
            List<DailyConfirmation> confirmations, Vehicle vehicle) {
        int capacity = vehicle == null ? Integer.MAX_VALUE : vehicle.getPassengerCapacity();
        List<DailyConfirmation> sorted = confirmations.stream()
                .sorted(Comparator.comparing(DailyConfirmation::getScheduledTime))
                .toList();
        List<List<DailyConfirmation>> batches = new ArrayList<>();
        List<DailyConfirmation> current = new ArrayList<>();
        LocalDateTime firstReturnPickup = null;
        for (DailyConfirmation confirmation : sorted) {
            LocalDateTime pickup = confirmation.getServiceDate().atTime(confirmation.getScheduledTime());
            boolean exceedsReturnWait = confirmation.getDirection() == Direction.VOLTA
                    && firstReturnPickup != null
                    && pickup.isAfter(firstReturnPickup.plus(properties.getMaximumReturnWait()));
            if (!current.isEmpty() && (current.size() >= capacity || exceedsReturnWait)) {
                batches.add(List.copyOf(current));
                current.clear();
                firstReturnPickup = null;
            }
            if (firstReturnPickup == null && confirmation.getDirection() == Direction.VOLTA) {
                firstReturnPickup = pickup;
            }
            current.add(confirmation);
        }
        if (!current.isEmpty()) {
            batches.add(List.copyOf(current));
        }
        return batches;
    }

    private void createTrip(
            List<DailyConfirmation> confirmations, Vehicle vehicle, LocalDateTime now) {
        DailyConfirmation first = confirmations.getFirst();
        RoutePlanningResult route = vehicle == null
                ? RoutePlanningResult.unavailable("O motorista não possui um veículo padrão")
                : optimize(confirmations, vehicle);
        Trip trip = Trip.builder()
                .driver(first.getDriver())
                .vehicle(vehicle)
                .serviceDate(first.getServiceDate())
                .direction(first.getDirection())
                .status(route.feasible() ? TripStatus.PLANNED : TripStatus.NEEDS_ATTENTION)
                .plannedDepartureAt(route.feasible()
                        ? route.departureAt() : first.getPreliminaryDepartureAt())
                .planningIssue(route.issue())
                .routeProvider(route.provider())
                .routeReference(route.reference())
                .encodedPolyline(route.encodedPolyline())
                .routeCalculatedAt(route.feasible() ? now : null)
                .createdAt(now)
                .build();

        Map<Long, RouteStopPlan> stops = route.stops().stream()
                .collect(Collectors.toMap(RouteStopPlan::confirmationId, stop -> stop));
        int fallbackOrder = 1;
        for (DailyConfirmation confirmation : confirmations) {
            RouteStopPlan stop = stops.get(confirmation.getId());
            trip.addParticipant(TripParticipant.builder()
                    .student(confirmation.getStudent())
                    .confirmation(confirmation)
                    .pickupOrder(stop == null ? fallbackOrder : stop.pickupOrder())
                    .dropoffOrder(stop == null ? fallbackOrder : stop.dropoffOrder())
                    .estimatedPickupAt(stop == null ? null : stop.estimatedPickupAt())
                    .estimatedDropoffAt(stop == null ? null : stop.estimatedDropoffAt())
                    .build());
            fallbackOrder++;
        }
        tripRepository.save(trip);
        if (route.feasible()) {
            notifyTripPlanned(trip);
        } else {
            notificationService.create(first.getDriver().getId(),
                    NotificationType.PLANNING_NEEDS_ATTENTION,
                    "Planejamento precisa de atenção",
                    route.issue(), trip, null);
        }
    }

    private RoutePlanningResult optimize(List<DailyConfirmation> confirmations, Vehicle vehicle) {
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
                first.getDirection() == Direction.IDA ? point(base) : null,
                first.getDirection() == Direction.VOLTA ? point(base) : null,
                passengers));
    }

    private RoutePassenger passenger(DailyConfirmation confirmation) {
        Address home = confirmation.getStudent().getUser().getAddress();
        Address institution = confirmation.getStudent().getInstitution().getAddress();
        LocalDateTime scheduled = confirmation.getServiceDate().atTime(confirmation.getScheduledTime());
        if (confirmation.getDirection() == Direction.IDA) {
            return new RoutePassenger(confirmation.getId(), confirmation.getStudent().getId(),
                    point(home), point(institution), null, null, scheduled);
        }
        return new RoutePassenger(confirmation.getId(), confirmation.getStudent().getId(),
                point(institution), point(home), scheduled,
                scheduled.plus(properties.getMaximumReturnWait()), null);
    }

    private void applyReplanning(
            Trip trip, Vehicle vehicle, RoutePlanningResult route, LocalDateTime calculatedAt) {
        trip.setVehicle(vehicle);
        trip.setStatus(route.feasible() ? TripStatus.PLANNED : TripStatus.NEEDS_ATTENTION);
        trip.setPlanningIssue(route.issue());
        trip.setRouteProvider(route.provider());
        trip.setRouteReference(route.reference());
        trip.setEncodedPolyline(route.encodedPolyline());
        trip.setRouteCalculatedAt(route.feasible() ? calculatedAt : null);
        if (!route.feasible()) {
            return;
        }
        trip.setPlannedDepartureAt(route.departureAt());
        Map<Long, RouteStopPlan> stops = route.stops().stream()
                .collect(Collectors.toMap(RouteStopPlan::confirmationId, stop -> stop));
        for (TripParticipant participant : trip.getParticipants()) {
            RouteStopPlan stop = stops.get(participant.getConfirmation().getId());
            if (stop == null) {
                throw new BusinessRuleException("A rota recalculada não contém todos os alunos");
            }
            participant.setPickupOrder(stop.pickupOrder());
            participant.setDropoffOrder(stop.dropoffOrder());
            participant.setEstimatedPickupAt(stop.estimatedPickupAt());
            participant.setEstimatedDropoffAt(stop.estimatedDropoffAt());
        }
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
