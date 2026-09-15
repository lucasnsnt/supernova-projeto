package ink.lucasnsnt.supernovaprojeto.dtos.trip;

import jakarta.validation.constraints.NotNull;

public record TripVehicleUpdateRequest(@NotNull Long vehicleId) {
}
