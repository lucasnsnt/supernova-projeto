package ink.lucasnsnt.supernovaprojeto.dtos.driver;

import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressResponse;
import ink.lucasnsnt.supernovaprojeto.models.Driver;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DriverResponse(
        Long id,
        String name,
        String email,
        String phone,
        LocalDate dateOfBirth,
        AddressResponse address,
        String cnh,
        DriverStatus status,
        String statusReason,
        LocalDateTime reviewedAt) {

    public static DriverResponse from(Driver driver) {
        var user = driver.getUser();
        return new DriverResponse(driver.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getDateOfBirth(), AddressResponse.from(user.getAddress()), driver.getCnh(),
                driver.getStatus(), driver.getStatusReason(), driver.getReviewedAt());
    }
}
