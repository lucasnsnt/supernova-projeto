package ink.lucasnsnt.supernovaprojeto.dtos.student;

import jakarta.validation.constraints.NotBlank;

public record InviteAcceptanceRequest(@NotBlank String token) {
}
