package ink.lucasnsnt.supernovaprojeto.dtos.route;

import ink.lucasnsnt.supernovaprojeto.models.RecurringRouteEnrollment;
import ink.lucasnsnt.supernovaprojeto.models.enums.RouteEnrollmentStatus;
import java.time.LocalDateTime;

public record RouteEnrollmentResponse(Long id, Long routeId, String routeName, String driverName,
                                      String studentName, boolean outboundEnabled, boolean returnEnabled,
                                      RouteEnrollmentStatus status, LocalDateTime requestedAt, LocalDateTime reviewedAt) {
    public static RouteEnrollmentResponse from(RecurringRouteEnrollment item) {
        return new RouteEnrollmentResponse(item.getId(), item.getRoute().getId(), item.getRoute().getName(),
                item.getRoute().getDriver().getUser().getName(), item.getStudent().getUser().getName(),
                item.isOutboundEnabled(), item.isReturnEnabled(), item.getStatus(), item.getRequestedAt(), item.getReviewedAt());
    }
}
