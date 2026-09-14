package ink.lucasnsnt.supernovaprojeto.dtos.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailUpdateRequest(
        @NotBlank @Email String email,
        @NotBlank String registrationToken) {
}
