package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.RecurringRouteEnrollment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RecurringRouteEnrollmentRepository extends JpaRepository<RecurringRouteEnrollment, Long> {
    @EntityGraph(attributePaths = {"route", "route.driver", "route.driver.user", "route.vehicle", "student", "student.user"})
    List<RecurringRouteEnrollment> findAllByRouteIdOrderByRequestedAtDesc(Long routeId);
    @EntityGraph(attributePaths = {"route", "route.driver", "route.driver.user", "route.vehicle", "student", "student.user"})
    List<RecurringRouteEnrollment> findAllByRouteIdAndActiveTrueOrderByRequestedAtDesc(Long routeId);
    @EntityGraph(attributePaths = {"route", "route.driver", "route.driver.user", "route.vehicle", "student", "student.user"})
    List<RecurringRouteEnrollment> findAllByStudentIdOrderByRequestedAtDesc(Long studentId);
    @EntityGraph(attributePaths = {"route", "route.driver", "route.driver.user", "route.vehicle", "student", "student.user"})
    List<RecurringRouteEnrollment> findAllByStudentIdAndActiveTrueOrderByRequestedAtDesc(Long studentId);
    Optional<RecurringRouteEnrollment> findByRouteIdAndStudentId(Long routeId, Long studentId);
    @EntityGraph(attributePaths = {"route", "route.driver", "route.driver.user", "route.schedules", "student", "student.user", "student.institution"})
    List<RecurringRouteEnrollment> findAllByActiveTrue();
}
