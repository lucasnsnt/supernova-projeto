package ink.lucasnsnt.supernovaprojeto.dtos.student;

import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record StudentProfileUpdateRequest(
        @NotBlank String name,
        @NotBlank String phone,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull @Valid AddressRequest address) {
}
