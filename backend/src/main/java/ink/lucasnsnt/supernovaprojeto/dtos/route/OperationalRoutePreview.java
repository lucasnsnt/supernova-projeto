package ink.lucasnsnt.supernovaprojeto.dtos.route;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.TripParticipantResponse;
import ink.lucasnsnt.supernovaprojeto.models.enums.*;
import java.time.*;
import java.util.List;
public record OperationalRoutePreview(Long routeId, String routeName, LocalDate serviceDate, Direction direction,
        LocalDateTime scheduledDepartureAt, LocalDateTime departureAt, boolean withinStartWindow,
        boolean canStart, String planningIssue, String encodedPolyline, Long tripId, TripStatus tripStatus,
        List<TripParticipantResponse> participants) {}
