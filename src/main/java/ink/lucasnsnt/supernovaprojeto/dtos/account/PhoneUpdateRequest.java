package ink.lucasnsnt.supernovaprojeto.dtos.account;

import jakarta.validation.constraints.NotBlank;

public record PhoneUpdateRequest(@NotBlank String phone) {
}
