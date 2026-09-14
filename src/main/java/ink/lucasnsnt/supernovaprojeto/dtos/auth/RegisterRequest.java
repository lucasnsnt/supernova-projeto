package ink.lucasnsnt.supernovaprojeto.dtos.auth;

import ink.lucasnsnt.supernovaprojeto.models.enums.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank String registrationToken,
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",
                message = "deve conter letra maiúscula, minúscula, número e caractere especial")
        String password,
        @NotBlank String phone,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull Role role,
        @NotNull @Valid AddressRegistrationRequest address,
        String cnh,
        String driverInviteToken) {

    @AssertTrue(message = "CNH é obrigatória para motorista e convite é permitido apenas para aluno")
    public boolean isRoleDataValid() {
        if (role == null || role == Role.ADMIN) {
            return false;
        }
        if (role == Role.DRIVER) {
            return cnh != null && !cnh.isBlank()
                    && (driverInviteToken == null || driverInviteToken.isBlank());
        }
        return cnh == null || cnh.isBlank();
    }
}
