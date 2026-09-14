package ink.lucasnsnt.supernovaprojeto.dtos.account;

import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressResponse;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;

import java.time.LocalDate;

public record AccountResponse(
        Long id,
        String name,
        String email,
        String phone,
        LocalDate dateOfBirth,
        Role role,
        AddressResponse address,
        DriverStatus driverStatus,
        String driverStatusReason,
        String cnh) {
}
