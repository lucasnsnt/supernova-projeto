package ink.lucasnsnt.supernovaprojeto.dtos.route;

import ink.lucasnsnt.supernovaprojeto.models.RecurringRouteEnrollment;
import java.time.LocalDateTime;

public record RouteEnrollmentResponse(Long id, Long routeId, String routeName, String driverName,
                                      String studentName, Long studentId, String institutionName, boolean outboundEnabled, boolean returnEnabled,
                                      LocalDateTime requestedAt) {
    public static RouteEnrollmentResponse from(RecurringRouteEnrollment item) {
        return new RouteEnrollmentResponse(item.getId(), item.getRoute().getId(), item.getRoute().getName(),
                item.getRoute().getDriver().getUser().getName(), item.getStudent().getUser().getName(),
                item.getStudent().getId(), item.getStudent().getInstitution() == null ? null : item.getStudent().getInstitution().getName(),
                item.isOutboundEnabled(), item.isReturnEnabled(), item.getRequestedAt());
    }
}
