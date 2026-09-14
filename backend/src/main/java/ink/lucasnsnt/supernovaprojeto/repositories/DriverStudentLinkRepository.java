package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.DriverStudentLink;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStudentLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverStudentLinkRepository extends JpaRepository<DriverStudentLink, Long> {

    Optional<DriverStudentLink> findFirstByDriverIdAndStudentIdAndStatus(
            Long driverId, Long studentId, DriverStudentLinkStatus status);

    Optional<DriverStudentLink> findFirstByStudentIdAndStatus(
            Long studentId, DriverStudentLinkStatus status);

    @EntityGraph(attributePaths = {
            "student", "student.user", "student.user.address", "student.institution", "student.schedules"
    })
    List<DriverStudentLink> findAllByDriverId(Long driverId);

    @EntityGraph(attributePaths = {"driver", "driver.user"})
    List<DriverStudentLink> findAllByStudentId(Long studentId);

    List<DriverStudentLink> findAllByDriverIdAndStatus(Long driverId, DriverStudentLinkStatus status);

    List<DriverStudentLink> findAllByStudentIdAndStatus(Long studentId, DriverStudentLinkStatus status);

    @EntityGraph(attributePaths = {
            "driver", "driver.user", "student", "student.user", "student.user.address",
            "student.institution", "student.institution.address", "student.schedules"
    })
    List<DriverStudentLink> findAllByStatus(DriverStudentLinkStatus status);
}
