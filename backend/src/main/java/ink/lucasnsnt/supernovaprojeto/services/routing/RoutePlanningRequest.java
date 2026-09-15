package ink.lucasnsnt.supernovaprojeto.services.routing;

import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;

import java.time.LocalDate;
import java.util.List;

public record RoutePlanningRequest(
        Long driverId,
        LocalDate serviceDate,
        Direction direction,
        int vehicleCapacity,
        RoutePoint start,
        RoutePoint end,
        List<RoutePassenger> passengers) {
}
