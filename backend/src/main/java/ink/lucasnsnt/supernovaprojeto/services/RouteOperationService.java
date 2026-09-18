package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.dtos.route.*;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.*;
import ink.lucasnsnt.supernovaprojeto.exceptions.*;
import ink.lucasnsnt.supernovaprojeto.models.*;
import ink.lucasnsnt.supernovaprojeto.models.enums.*;
import ink.lucasnsnt.supernovaprojeto.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Service
@RequiredArgsConstructor
public class RouteOperationService {
    private final RecurringRouteRepository routes;
    private final TripRepository trips;
    private final DriverRepository drivers;
    private final DailyConfirmationRepository confirmations;
    private final DriverService driverService;
    private final TripPlanningService planning;
    private final Clock clock;

    @Transactional
    public OperationalRoutePreview preview(Long driverId, Long routeId, LocalDate date, Direction direction) {
        driverService.requireOperationalView(driverId);
        RecurringRoute route = owned(driverId, routeId);
        LocalDateTime scheduled = scheduled(route, date, direction);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime departure = date.equals(now.toLocalDate()) ? now : scheduled;
        Trip existing = trips.findByRecurringRouteIdAndServiceDateAndDirection(routeId, date, direction).orElse(null);
        boolean terminal = existing != null && (existing.getStartedAt() != null || existing.getStatus() == TripStatus.CANCELLED);
        Trip result = terminal ? existing : planning.build(route, date, direction, scheduled, departure, planning.confirmed(routeId, date, direction));
        boolean canStart = !terminal && date.equals(now.toLocalDate()) && result.getStatus() == TripStatus.PLANNED
                && !trips.existsByDriverIdAndStatus(driverId, TripStatus.IN_PROGRESS);
        String issue = result.getPlanningIssue();
        if (!terminal && !date.equals(now.toLocalDate())) issue = "A viagem só pode ser iniciada no dia da saída";
        else if (!terminal && trips.existsByDriverIdAndStatus(driverId, TripStatus.IN_PROGRESS)) issue = "Já existe uma viagem em andamento";
        return new OperationalRoutePreview(routeId, route.getName(), date, direction, scheduled,
                terminal ? result.getDriverDepartureAt() : departure, within(now, scheduled), canStart, issue,
                result.getEncodedPolyline(), existing == null ? null : existing.getId(), existing == null ? null : existing.getStatus(),
                result.getParticipants().stream().map(p -> TripParticipantResponse.from(p, direction)).toList());
    }

    @Transactional
    public TripResponse start(Long driverId, Long routeId, RouteStartRequest request) {
        drivers.lockById(driverId);
        driverService.requireApproved(driverId);
        RecurringRoute route = owned(driverId, routeId);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime scheduled = scheduled(route, request.serviceDate(), request.direction());
        if (!request.serviceDate().equals(now.toLocalDate())) throw new BusinessRuleException("A viagem só pode ser iniciada no dia da saída");
        Trip existing = trips.findByRecurringRouteIdAndServiceDateAndDirection(routeId, request.serviceDate(), request.direction()).orElse(null);
        if (existing != null && existing.getStatus() == TripStatus.IN_PROGRESS) return TripResponse.from(existing);
        if (existing != null && (existing.getStartedAt() != null || existing.getStatus() == TripStatus.CANCELLED)) throw new BusinessRuleException("Esta saída já foi encerrada");
        if (trips.existsByDriverIdAndStatus(driverId, TripStatus.IN_PROGRESS)) throw new BusinessRuleException("Conclua a viagem em andamento antes de iniciar outra");
        if (!within(now, scheduled) && !request.acknowledgeOutsideWindow()) throw new BusinessRuleException("Confirme o início fora da janela de 30 minutos antes ou depois do horário previsto");
        Trip planned = planning.build(route, request.serviceDate(), request.direction(), scheduled, now,
                planning.confirmed(routeId, request.serviceDate(), request.direction()));
        if (planned.getStatus() != TripStatus.PLANNED) throw new BusinessRuleException(planned.getPlanningIssue());
        Trip trip = existing == null ? planned : existing;
        if (existing != null) planning.merge(existing, planned);
        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setStartedAt(now);
        trip.setDriverDepartureAt(now);
        trips.saveAndFlush(trip);
        // Freeze unanswered seats too: late answers must never modify a started manifest.
        confirmations.findAllByRecurringRouteIdAndServiceDateAndDirection(routeId, request.serviceDate(), request.direction())
                .stream().filter(c -> c.getStatus() == DailyConfirmationStatus.PENDING)
                .forEach(c -> c.setStatus(DailyConfirmationStatus.NO_RESPONSE));
        return TripResponse.from(trip);
    }

    @Transactional
    public TripResponse startTrip(Long driverId, Long tripId, boolean acknowledgeOutsideWindow) {
        drivers.lockById(driverId);
        Trip trip = trips.findByIdAndDriverId(tripId, driverId).orElseThrow(() -> new ResourceNotFoundException("Viagem", tripId));
        if (trip.getRecurringRoute() == null) throw new BusinessRuleException("Abra a prévia de uma rota do motorista para iniciar");
        return start(driverId, trip.getRecurringRoute().getId(), new RouteStartRequest(trip.getServiceDate(), trip.getDirection(), acknowledgeOutsideWindow));
    }

    private RecurringRoute owned(Long driverId, Long routeId) {
        RecurringRoute route = routes.findByIdAndDriverId(routeId, driverId).orElseThrow(() -> new ResourceNotFoundException("Rota", routeId));
        if (!route.isActive()) throw new BusinessRuleException("Esta rota está inativa");
        return route;
    }
    private LocalDateTime scheduled(RecurringRoute route, LocalDate date, Direction direction) {
        var schedules = route.getSchedules().stream().filter(s -> s.getDayOfWeek() == date.getDayOfWeek() && s.getDirection() == direction).toList();
        if (schedules.size() != 1) throw new BusinessRuleException("A rota precisa ter exatamente um horário para este dia e direção");
        return date.atTime(schedules.getFirst().getDepartureTime());
    }
    static boolean within(LocalDateTime now, LocalDateTime scheduled) {
        return !now.isBefore(scheduled.minusMinutes(30)) && !now.isAfter(scheduled.plusMinutes(30));
    }
}
