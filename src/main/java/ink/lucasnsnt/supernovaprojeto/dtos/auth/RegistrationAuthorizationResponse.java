package ink.lucasnsnt.supernovaprojeto.dtos.auth;

import java.time.LocalDateTime;

public record RegistrationAuthorizationResponse(
        String registrationToken,
        LocalDateTime expiresAt) {
}
