package ink.lucasnsnt.supernovaprojeto.dtos.invite;

import jakarta.validation.constraints.NotBlank;

public record InviteTokenRequest(@NotBlank String token) {
}
