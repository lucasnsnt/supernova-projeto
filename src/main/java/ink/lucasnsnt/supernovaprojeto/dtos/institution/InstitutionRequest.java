package ink.lucasnsnt.supernovaprojeto.dtos.institution;

import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressRequest;
import ink.lucasnsnt.supernovaprojeto.models.enums.InstitutionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InstitutionRequest(
        @NotBlank String name,
        @NotNull InstitutionType type,
        @NotNull @Valid AddressRequest address) {
}
