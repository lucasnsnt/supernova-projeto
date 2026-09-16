package ink.lucasnsnt.supernovaprojeto.dtos.account;

import jakarta.validation.constraints.NotBlank;

public record AccountProfileUpdateRequest(@NotBlank String name, @NotBlank String phone) {
}
