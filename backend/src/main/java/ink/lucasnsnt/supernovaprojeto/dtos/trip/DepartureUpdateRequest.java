package ink.lucasnsnt.supernovaprojeto.dtos.trip;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record DepartureUpdateRequest(
        @NotNull LocalDateTime departureAt,
        @Size(max = 500) String reason) {
}
