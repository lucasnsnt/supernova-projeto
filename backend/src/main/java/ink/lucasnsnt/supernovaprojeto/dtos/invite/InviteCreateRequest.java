package ink.lucasnsnt.supernovaprojeto.dtos.invite;

import jakarta.validation.constraints.Positive;

public record InviteCreateRequest(
        @Positive Integer validityDays,
        boolean replaceCurrent) {
}
