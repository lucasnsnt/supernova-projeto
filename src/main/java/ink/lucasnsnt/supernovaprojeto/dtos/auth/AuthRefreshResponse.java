package ink.lucasnsnt.supernovaprojeto.dtos.auth;

import java.time.Instant;

public record AuthRefreshResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt) {
}
