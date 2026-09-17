package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.RecurringRouteEnrollment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import ink.lucasnsnt.supernovaprojeto.models.enums.RouteEnrollmentStatus;

public interface RecurringRouteEnrollmentRepository extends JpaRepository<RecurringRouteEnrollment, Long> {
    @EntityGraph(attributePaths = {"route", "route.driver", "route.driver.user", "route.vehicle", "student", "student.user"})
    List<RecurringRouteEnrollment> findAllByRouteIdOrderByRequestedAtDesc(Long routeId);
    @EntityGraph(attributePaths = {"route", "route.driver", "route.driver.user", "route.vehicle", "student", "student.user"})
    List<RecurringRouteEnrollment> findAllByStudentIdOrderByRequestedAtDesc(Long studentId);
    Optional<RecurringRouteEnrollment> findByRouteIdAndStudentId(Long routeId, Long studentId);
    @EntityGraph(attributePaths = {"route", "route.driver", "route.driver.user", "route.schedules", "student", "student.user", "student.institution"})
    List<RecurringRouteEnrollment> findAllByStatus(RouteEnrollmentStatus status);
}
