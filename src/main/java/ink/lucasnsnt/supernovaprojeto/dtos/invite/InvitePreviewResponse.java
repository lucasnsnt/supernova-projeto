package ink.lucasnsnt.supernovaprojeto.dtos.invite;

import java.time.LocalDateTime;

public record InvitePreviewResponse(Long driverId, String driverName, LocalDateTime expiresAt) {
}
