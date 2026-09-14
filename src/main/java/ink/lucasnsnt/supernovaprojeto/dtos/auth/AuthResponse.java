package ink.lucasnsnt.supernovaprojeto.dtos.auth;

import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        Long userId,
        String email,
        Role role,
        DriverStatus driverStatus,
        boolean studentProfileComplete) {
}
