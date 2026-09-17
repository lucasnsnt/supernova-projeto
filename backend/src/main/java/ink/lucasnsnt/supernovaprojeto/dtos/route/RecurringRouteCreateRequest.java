package ink.lucasnsnt.supernovaprojeto.dtos.route;

import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record RecurringRouteCreateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull Long vehicleId,
        @NotEmpty List<@Valid Schedule> schedules,
        @NotEmpty List<@Valid InstitutionStop> institutions) {
    public record Schedule(@NotNull DayOfWeek dayOfWeek, @NotNull Direction direction,
                           @NotNull LocalTime departureTime, @NotNull LocalTime responseDeadlineTime) { }
    public record InstitutionStop(@NotNull Long institutionId, @NotNull @Positive Integer stopOrder) { }
}
