package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.EmailVerification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select verification from EmailVerification verification where verification.email = :email")
    Optional<EmailVerification> findByEmailForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select verification from EmailVerification verification "
            + "where verification.registrationTokenHash = :tokenHash")
    Optional<EmailVerification> findByRegistrationTokenHashForUpdate(
            @Param("tokenHash") String tokenHash);

    long deleteAllByUsedAtIsNullAndCreatedAtBefore(LocalDateTime cutoff);
}
