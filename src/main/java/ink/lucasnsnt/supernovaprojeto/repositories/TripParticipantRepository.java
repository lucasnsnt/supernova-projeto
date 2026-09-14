package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.TripParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripParticipantRepository extends JpaRepository<TripParticipant, Long> {

    boolean existsByConfirmationId(Long confirmationId);
}
