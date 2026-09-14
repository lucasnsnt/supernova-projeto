package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.config.SecurityProperties;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.models.RefreshToken;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.models.enums.RefreshTokenStatus;
import ink.lucasnsnt.supernovaprojeto.repositories.RefreshTokenRepository;
import ink.lucasnsnt.supernovaprojeto.security.IssuedTokens;
import ink.lucasnsnt.supernovaprojeto.security.JwtTokenService;
import ink.lucasnsnt.supernovaprojeto.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final TokenHasher tokenHasher;
    private final SecurityProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public IssuedTokens start(User user) {
        invalidateActiveSessions(user.getId(), RefreshTokenStatus.REVOKED);
        return issue(user);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public IssuedTokens rotate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BusinessRuleException("Sessão inválida ou expirada");
        }
        RefreshToken current = refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawRefreshToken))
                .orElseThrow(() -> new BusinessRuleException("Sessão inválida ou expirada"));
        LocalDateTime now = LocalDateTime.now(clock);

        if (current.getStatus() != RefreshTokenStatus.ACTIVE) {
            invalidateActiveSessions(current.getUser().getId(), RefreshTokenStatus.REVOKED);
            throw new BusinessRuleException("Sessão inválida ou expirada");
        }
        if (!current.getExpiresAt().isAfter(now)) {
            current.setStatus(RefreshTokenStatus.EXPIRED);
            current.setInvalidatedAt(now);
            throw new BusinessRuleException("Sessão inválida ou expirada");
        }

        current.setStatus(RefreshTokenStatus.ROTATED);
        current.setInvalidatedAt(now);
        return issue(current.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawRefreshToken))
                .filter(token -> token.getStatus() == RefreshTokenStatus.ACTIVE)
                .ifPresent(token -> {
                    token.setStatus(RefreshTokenStatus.REVOKED);
                    token.setInvalidatedAt(LocalDateTime.now(clock));
                });
    }

    private IssuedTokens issue(User user) {
        String rawRefreshToken = generateRefreshToken();
        Instant refreshExpiresAt = clock.instant().plus(properties.getRefreshTokenTtl());
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHasher.hash(rawRefreshToken))
                .createdAt(LocalDateTime.now(clock))
                .expiresAt(LocalDateTime.ofInstant(refreshExpiresAt, clock.getZone()))
                .status(RefreshTokenStatus.ACTIVE)
                .build();
        refreshTokenRepository.save(refreshToken);

        JwtTokenService.AccessToken accessToken = jwtTokenService.issue(user);
        return new IssuedTokens(
                accessToken.value(), accessToken.expiresAt(), rawRefreshToken,
                refreshExpiresAt, user);
    }

    private void invalidateActiveSessions(Long userId, RefreshTokenStatus newStatus) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (RefreshToken token : refreshTokenRepository
                .findAllByUserIdAndStatus(userId, RefreshTokenStatus.ACTIVE)) {
            token.setStatus(newStatus);
            token.setInvalidatedAt(now);
        }
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
