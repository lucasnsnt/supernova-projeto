package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.DriverInvite;
import ink.lucasnsnt.supernovaprojeto.models.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverInviteRepository extends JpaRepository<DriverInvite, Long> {

    Optional<DriverInvite> findByToken(String token);

    List<DriverInvite> findAllByDriverId(Long driverId);

    List<DriverInvite> findAllByDriverIdAndStatus(Long driverId, InviteStatus status);

    Optional<DriverInvite> findFirstByDriverIdAndStatus(Long driverId, InviteStatus status);
}
