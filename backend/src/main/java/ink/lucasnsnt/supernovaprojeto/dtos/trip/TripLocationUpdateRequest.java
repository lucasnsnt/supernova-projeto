package ink.lucasnsnt.supernovaprojeto.dtos.trip;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record TripLocationUpdateRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @PositiveOrZero Double accuracy,
        @DecimalMin("0.0") @DecimalMax("360.0") Double heading,
        LocalDateTime recordedAt) {
}
