package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.config.SecurityProperties;
import ink.lucasnsnt.supernovaprojeto.dtos.auth.RegistrationAuthorizationResponse;
import ink.lucasnsnt.supernovaprojeto.events.EmailVerificationRequestedEvent;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceConflictException;
import ink.lucasnsnt.supernovaprojeto.models.EmailVerification;
import ink.lucasnsnt.supernovaprojeto.repositories.EmailVerificationRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import ink.lucasnsnt.supernovaprojeto.security.TokenHasher;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

@Service
@Validated
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenHasher tokenHasher;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestCode(@NotBlank @Email String email) {
        String normalizedEmail = normalize(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        EmailVerification verification = verificationRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> EmailVerification.builder()
                        .email(normalizedEmail)
                        .createdAt(now)
                        .build());

        if (verification.getLastSentAt() != null
                && verification.getLastSentAt().plus(properties.getVerificationResendCooldown()).isAfter(now)) {
            throw new BusinessRuleException("Aguarde antes de solicitar outro código");
        }

        String code = "%06d".formatted(secureRandom.nextInt(1_000_000));
        verification.setCodeHash(passwordEncoder.encode(code));
        verification.setExpiresAt(now.plus(properties.getVerificationCodeTtl()));
        verification.setAttemptCount(0);
        verification.setLastSentAt(now);
        verification.setVerifiedAt(null);
        verification.setRegistrationTokenHash(null);
        verification.setRegistrationTokenExpiresAt(null);
        verification.setUsedAt(null);
        verificationRepository.save(verification);
        eventPublisher.publishEvent(new EmailVerificationRequestedEvent(normalizedEmail, code));
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public RegistrationAuthorizationResponse confirmCode(
            @NotBlank @Email String email,
            @NotBlank String code) {
        String normalizedEmail = normalize(email);
        EmailVerification verification = verificationRepository.findByEmailForUpdate(normalizedEmail)
                .orElseThrow(() -> new BusinessRuleException("Código inválido ou expirado"));
        LocalDateTime now = LocalDateTime.now(clock);

        if (!verification.getExpiresAt().isAfter(now)
                || verification.getAttemptCount() >= properties.getMaxVerificationAttempts()) {
            throw new BusinessRuleException("Código inválido ou expirado");
        }
        if (!passwordEncoder.matches(code, verification.getCodeHash())) {
            verification.setAttemptCount(verification.getAttemptCount() + 1);
            throw new BusinessRuleException("Código inválido ou expirado");
        }

        String registrationToken = generateToken();
        LocalDateTime tokenExpiresAt = now.plus(properties.getRegistrationTokenTtl());
        verification.setVerifiedAt(now);
        verification.setRegistrationTokenHash(tokenHasher.hash(registrationToken));
        verification.setRegistrationTokenExpiresAt(tokenExpiresAt);
        return new RegistrationAuthorizationResponse(registrationToken, tokenExpiresAt);
    }

    @Transactional
    public LocalDateTime consumeRegistrationAuthorization(String email, String registrationToken) {
        String normalizedEmail = normalize(email);
        EmailVerification verification = verificationRepository
                .findByRegistrationTokenHashForUpdate(tokenHasher.hash(registrationToken))
                .orElseThrow(() -> new BusinessRuleException("Autorização de cadastro inválida ou expirada"));
        LocalDateTime now = LocalDateTime.now(clock);

        if (!verification.getEmail().equals(normalizedEmail)
                || verification.getVerifiedAt() == null
                || verification.getUsedAt() != null
                || !verification.getRegistrationTokenExpiresAt().isAfter(now)) {
            throw new BusinessRuleException("Autorização de cadastro inválida ou expirada");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceConflictException("O e-mail já está cadastrado");
        }

        verification.setUsedAt(now);
        return verification.getVerifiedAt();
    }

    public String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
