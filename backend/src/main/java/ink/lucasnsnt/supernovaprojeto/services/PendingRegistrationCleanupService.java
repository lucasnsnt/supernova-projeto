package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.config.SecurityProperties;
import ink.lucasnsnt.supernovaprojeto.repositories.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PendingRegistrationCleanupService {

    private final EmailVerificationRepository verificationRepository;
    private final SecurityProperties properties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.security.pending-registration-cleanup-interval:1h}")
    @Transactional
    public void removeAbandonedRegistrations() {
        LocalDateTime cutoff = LocalDateTime.now(clock)
                .minus(properties.getPendingRegistrationRetention());
        verificationRepository.deleteAllByUsedAtIsNullAndCreatedAtBefore(cutoff);
    }
}
