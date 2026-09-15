package ink.lucasnsnt.supernovaprojeto.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationRequest(
        @NotBlank @Email String email) {
}
