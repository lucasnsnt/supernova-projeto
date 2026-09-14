package ink.lucasnsnt.supernovaprojeto.dtos.student;

import jakarta.validation.constraints.NotNull;

public record InstitutionSelectionRequest(@NotNull Long institutionId) {
}
