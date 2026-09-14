package ink.lucasnsnt.supernovaprojeto.dtos.driver;

import jakarta.validation.constraints.NotBlank;

public record StatusReasonRequest(@NotBlank String reason) {
}
