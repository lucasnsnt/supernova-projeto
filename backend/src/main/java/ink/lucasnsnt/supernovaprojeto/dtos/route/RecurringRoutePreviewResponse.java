package ink.lucasnsnt.supernovaprojeto.dtos.route;

import ink.lucasnsnt.supernovaprojeto.models.RecurringRouteEnrollment;
import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record RecurringRoutePreviewResponse(Long routeId, String routeName, LocalDate serviceDate,
                                            Direction direction, LocalTime departureTime,
                                            List<Stop> stops, List<Passenger> passengers) {
    public record Stop(String institutionName, Integer order, LocalTime expectedAt) { }
    public record Passenger(Long studentId, String studentName, String institutionName) { }

    public static RecurringRoutePreviewResponse from(LocalDate date, Direction direction, LocalTime departure,
                                                     List<RecurringRouteEnrollment> enrollments) {
        return from(enrollments.getFirst().getRoute(), date, direction, departure, enrollments);
    }

    public static RecurringRoutePreviewResponse from(ink.lucasnsnt.supernovaprojeto.models.RecurringRoute route,
            LocalDate date, Direction direction, LocalTime departure, List<RecurringRouteEnrollment> enrollments) {
        var stops = route.getInstitutions().stream().map(stop -> new Stop(stop.getInstitution().getName(), stop.getStopOrder(),
                direction == Direction.IDA ? stop.getOutboundArrivalBy() : stop.getReturnDepartureAt())).toList();
        var passengers = enrollments.stream().map(item -> new Passenger(item.getStudent().getId(), item.getStudent().getUser().getName(),
                item.getStudent().getInstitution() == null ? null : item.getStudent().getInstitution().getName())).toList();
        return new RecurringRoutePreviewResponse(route.getId(), route.getName(), date, direction, departure, stops, passengers);
    }
}
