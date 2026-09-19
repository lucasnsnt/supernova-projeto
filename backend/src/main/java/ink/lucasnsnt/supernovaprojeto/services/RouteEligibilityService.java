package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.models.*;
import ink.lucasnsnt.supernovaprojeto.models.enums.*;
import ink.lucasnsnt.supernovaprojeto.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteEligibilityService {
    private final DriverStudentLinkRepository links;
    private final RecurringRouteEnrollmentRepository enrollments;

    public boolean eligible(Student student, RecurringRoute route) {
        return route != null && route.isActive() && route.getDriver().getStatus() == DriverStatus.APPROVED
                && student.getInstitution() != null
                && route.getInstitutions().stream().anyMatch(stop -> stop.getInstitution().getId().equals(student.getInstitution().getId()))
                && links.findFirstByStudentIdAndStatus(student.getId(), DriverStudentLinkStatus.ACTIVE)
                .map(link -> link.getDriver().getId().equals(route.getDriver().getId())).orElse(false);
    }

    public boolean eligible(DailyConfirmation confirmation) {
        var route = confirmation.getRecurringRoute();
        if (!eligible(confirmation.getStudent(), route)) return false;
        return enrollments.findByRouteIdAndStudentId(route.getId(), confirmation.getStudent().getId())
                .filter(RecurringRouteEnrollment::isActive)
                .filter(item -> confirmation.getDirection() == Direction.IDA ? item.isOutboundEnabled() : item.isReturnEnabled())
                .isPresent();
    }
}
