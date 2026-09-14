package ink.lucasnsnt.supernovaprojeto.dtos.vehicle;

import ink.lucasnsnt.supernovaprojeto.models.Vehicle;

public record VehicleResponse(
        Long id, String brand, String model, Integer year, String licensePlate,
        Integer passengerCapacity, String color, boolean defaultVehicle) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getBrand(), vehicle.getModel(),
                vehicle.getYear(), vehicle.getLicensePlate(), vehicle.getPassengerCapacity(), vehicle.getColor(),
                vehicle.isDefaultVehicle());
    }
}
