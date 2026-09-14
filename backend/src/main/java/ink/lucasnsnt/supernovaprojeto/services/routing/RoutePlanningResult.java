package ink.lucasnsnt.supernovaprojeto.services.routing;

import java.time.LocalDateTime;
import java.util.List;

public record RoutePlanningResult(
        boolean feasible,
        String provider,
        String reference,
        LocalDateTime departureAt,
        String encodedPolyline,
        String issue,
        List<RouteStopPlan> stops) {

    public static RoutePlanningResult unavailable(String issue) {
        return new RoutePlanningResult(false, null, null, null, null, issue, List.of());
    }
}
