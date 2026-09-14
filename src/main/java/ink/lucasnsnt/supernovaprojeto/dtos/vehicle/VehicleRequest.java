package ink.lucasnsnt.supernovaprojeto.dtos.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record VehicleRequest(
        @NotBlank String brand,
        @NotBlank String model,
        Integer year,
        @NotBlank String licensePlate,
        @Positive int passengerCapacity,
        String color) {
}
