package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceConflictException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.models.Driver;
import ink.lucasnsnt.supernovaprojeto.models.Vehicle;
import ink.lucasnsnt.supernovaprojeto.repositories.VehicleRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverService driverService;

    @Transactional
    public Vehicle create(
            @NotNull Long driverId,
            @NotBlank String brand,
            @NotBlank String model,
            Integer year,
            @NotBlank String licensePlate,
            @Positive int passengerCapacity,
            String color) {
        Driver driver = driverService.requireApproved(driverId);
        if (vehicleRepository.existsByLicensePlateIgnoreCase(licensePlate)) {
            throw new ResourceConflictException("A placa já está cadastrada");
        }

        Vehicle vehicle = Vehicle.builder()
                .brand(brand)
                .model(model)
                .year(year)
                .licensePlate(licensePlate.toUpperCase())
                .passengerCapacity(passengerCapacity)
                .color(color)
                .build();
        driver.addVehicle(vehicle);
        return vehicleRepository.save(vehicle);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findAllByDriver(@NotNull Long driverId) {
        driverService.requireApproved(driverId);
        return vehicleRepository.findAllByDriverId(driverId);
    }

    @Transactional
    public void delete(@NotNull Long driverId, @NotNull Long vehicleId) {
        driverService.requireApproved(driverId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo", vehicleId));
        if (!vehicle.getDriver().getId().equals(driverId)) {
            throw new ResourceNotFoundException("Veículo", vehicleId);
        }
        vehicleRepository.delete(vehicle);
    }
}
