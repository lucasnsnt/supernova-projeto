package ink.lucasnsnt.supernovaprojeto.dtos.trip;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TripCancellationRequest(@NotBlank @Size(max = 500) String reason) {
}
