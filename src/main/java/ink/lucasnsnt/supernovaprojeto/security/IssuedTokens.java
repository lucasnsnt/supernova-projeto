package ink.lucasnsnt.supernovaprojeto.security;

import ink.lucasnsnt.supernovaprojeto.models.User;

import java.time.Instant;

public record IssuedTokens(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        User user) {
}
