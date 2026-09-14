package ink.lucasnsnt.supernovaprojeto.dtos.invite;

import ink.lucasnsnt.supernovaprojeto.models.DriverInvite;
import ink.lucasnsnt.supernovaprojeto.models.enums.InviteStatus;

import java.time.LocalDateTime;

public record InviteResponse(
        Long id,
        String token,
        InviteStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        LocalDateTime expiredAt) {

    public static InviteResponse from(DriverInvite invite) {
        return new InviteResponse(invite.getId(), invite.getToken(), invite.getStatus(),
                invite.getCreatedAt(), invite.getExpiresAt(), invite.getRevokedAt(), invite.getExpiredAt());
    }
}
