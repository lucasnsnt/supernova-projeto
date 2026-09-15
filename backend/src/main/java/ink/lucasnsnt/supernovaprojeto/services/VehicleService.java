package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceConflictException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.models.Driver;
import ink.lucasnsnt.supernovaprojeto.models.Vehicle;
import ink.lucasnsnt.supernovaprojeto.repositories.VehicleRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.TripRepository;
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
    private final TripRepository tripRepository;
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
                .defaultVehicle(!vehicleRepository.existsByDriverIdAndDefaultVehicleTrue(driverId))
                .build();
        driver.addVehicle(vehicle);
        return vehicleRepository.save(vehicle);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findAllByDriver(@NotNull Long driverId) {
        driverService.requireOperationalView(driverId);
        return vehicleRepository.findAllByDriverId(driverId);
    }

    @Transactional
    public Vehicle update(
            @NotNull Long driverId,
            @NotNull Long vehicleId,
            @NotBlank String brand,
            @NotBlank String model,
            Integer year,
            @NotBlank String licensePlate,
            @Positive int passengerCapacity,
            String color) {
        driverService.requireApproved(driverId);
        Vehicle vehicle = findOwnedVehicle(driverId, vehicleId);
        vehicleRepository.findByLicensePlateIgnoreCase(licensePlate)
                .filter(found -> !found.getId().equals(vehicleId))
                .ifPresent(found -> {
                    throw new ResourceConflictException("A placa já está cadastrada");
                });
        vehicle.setBrand(brand.trim());
        vehicle.setModel(model.trim());
        vehicle.setYear(year);
        vehicle.setLicensePlate(licensePlate.trim().toUpperCase());
        vehicle.setPassengerCapacity(passengerCapacity);
        vehicle.setColor(color);
        return vehicle;
    }

    @Transactional
    public void delete(@NotNull Long driverId, @NotNull Long vehicleId) {
        driverService.requireApproved(driverId);
        Vehicle vehicle = findOwnedVehicle(driverId, vehicleId);
        if (tripRepository.existsByVehicleId(vehicleId)) {
            throw new BusinessRuleException("Um veículo associado a viagens não pode ser removido");
        }
        boolean wasDefault = vehicle.isDefaultVehicle();
        vehicleRepository.delete(vehicle);
        vehicleRepository.flush();
        if (wasDefault) {
            vehicleRepository.findFirstByDriverIdOrderByIdAsc(driverId)
                    .ifPresent(replacement -> replacement.setDefaultVehicle(true));
        }
    }

    @Transactional
    public Vehicle setDefault(@NotNull Long driverId, @NotNull Long vehicleId) {
        driverService.requireApproved(driverId);
        Vehicle selected = findOwnedVehicle(driverId, vehicleId);
        vehicleRepository.findFirstByDriverIdAndDefaultVehicleTrue(driverId)
                .filter(current -> !current.getId().equals(vehicleId))
                .ifPresent(current -> current.setDefaultVehicle(false));
        selected.setDefaultVehicle(true);
        return selected;
    }

    @Transactional(readOnly = true)
    public Vehicle findDefault(@NotNull Long driverId) {
        return vehicleRepository.findFirstByDriverIdAndDefaultVehicleTrue(driverId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Defina um veículo padrão antes de planejar viagens"));
    }

    private Vehicle findOwnedVehicle(Long driverId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo", vehicleId));
        if (!vehicle.getDriver().getId().equals(driverId)) {
            throw new ResourceNotFoundException("Veículo", vehicleId);
        }
        return vehicle;
    }
}
