package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.DailyConfirmation;
import ink.lucasnsnt.supernovaprojeto.models.enums.DailyConfirmationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyConfirmationRepository extends JpaRepository<DailyConfirmation, Long> {

    boolean existsByStudentIdAndServiceDateAndDirectionAndRecurringRouteId(Long studentId, LocalDate date,
            ink.lucasnsnt.supernovaprojeto.models.enums.Direction direction, Long routeId);

    List<DailyConfirmation> findAllByRecurringRouteIdAndServiceDateAndDirection(Long routeId, LocalDate date,
            ink.lucasnsnt.supernovaprojeto.models.enums.Direction direction);

    @EntityGraph(attributePaths = {"driver", "driver.user", "student", "student.user", "student.institution"})
    List<DailyConfirmation> findAllByStudentIdAndServiceDateOrderByScheduledTime(
            Long studentId, LocalDate serviceDate);

    @EntityGraph(attributePaths = {"driver", "student", "student.user", "student.institution"})
    List<DailyConfirmation> findAllByDriverIdAndServiceDateOrderByScheduledTime(
            Long driverId, LocalDate serviceDate);

    Optional<DailyConfirmation> findByIdAndStudentId(Long id, Long studentId);

    List<DailyConfirmation> findAllByStatusAndResponseDeadlineLessThanEqual(
            DailyConfirmationStatus status, LocalDateTime deadline);

    @EntityGraph(attributePaths = {
            "driver", "driver.user", "driver.user.address", "driver.operationalAddress",
            "student", "student.user", "student.user.address",
            "student.institution", "student.institution.address"
    })
    List<DailyConfirmation> findAllByStatusAndResponseDeadlineLessThanEqualOrderByResponseDeadline(
            DailyConfirmationStatus status, LocalDateTime deadline);

    boolean existsByStudentIdAndServiceDateAndDirectionAndScheduledTime(
            Long studentId, LocalDate serviceDate,
            ink.lucasnsnt.supernovaprojeto.models.enums.Direction direction,
            java.time.LocalTime scheduledTime);

    boolean existsByStudentIdAndServiceDateAndDirection(
            Long studentId, LocalDate serviceDate,
            ink.lucasnsnt.supernovaprojeto.models.enums.Direction direction);
}
