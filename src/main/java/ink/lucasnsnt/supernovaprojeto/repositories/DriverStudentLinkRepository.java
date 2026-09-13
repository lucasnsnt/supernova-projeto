package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.DriverStudentLink;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStudentLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverStudentLinkRepository extends JpaRepository<DriverStudentLink, Long> {

    Optional<DriverStudentLink> findByDriverIdAndStudentId(Long driverId, Long studentId);

    List<DriverStudentLink> findAllByDriverId(Long driverId);

    List<DriverStudentLink> findAllByStudentId(Long studentId);

    List<DriverStudentLink> findAllByDriverIdAndStatus(Long driverId, DriverStudentLinkStatus status);

    List<DriverStudentLink> findAllByStudentIdAndStatus(Long studentId, DriverStudentLinkStatus status);
}
