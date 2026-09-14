package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceConflictException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.models.Driver;
import ink.lucasnsnt.supernovaprojeto.models.DriverInvite;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.models.Address;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.InviteStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;
import ink.lucasnsnt.supernovaprojeto.repositories.DriverInviteRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.DriverRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.AddressRepository;
import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.driver.DriverResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final DriverInviteRepository inviteRepository;
    private final AddressRepository addressRepository;
    private final Clock clock;

    @Transactional
    public Driver register(@NotNull Long userId, @NotBlank String cnh) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));

        if (user.getRole() != Role.DRIVER) {
            throw new BusinessRuleException("Somente um usuário com papel DRIVER pode virar motorista");
        }
        if (driverRepository.existsById(userId)) {
            throw new ResourceConflictException("O usuário já possui um cadastro de motorista");
        }
        if (driverRepository.existsByCnh(cnh)) {
            throw new ResourceConflictException("A CNH já está cadastrada");
        }

        Driver driver = Driver.builder()
                .user(user)
                .cnh(cnh)
                .status(DriverStatus.PENDING)
                .build();
        user.setDriver(driver);
        return driverRepository.save(driver);
    }

    @Transactional(readOnly = true)
    public Driver findById(@NotNull Long driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista", driverId));
    }

    @Transactional
    public Driver approve(@NotNull Long driverId) {
        Driver driver = findById(driverId);
        requireStatus(driver, DriverStatus.PENDING);
        driver.setStatus(DriverStatus.APPROVED);
        driver.setStatusReason(null);
        driver.setReviewedAt(LocalDateTime.now(clock));
        return driver;
    }

    @Transactional
    public DriverResponse approveResponse(@NotNull Long driverId) {
        return DriverResponse.from(approve(driverId));
    }

    @Transactional
    public Driver reject(@NotNull Long driverId, @NotBlank String reason) {
        Driver driver = findById(driverId);
        requireStatus(driver, DriverStatus.PENDING);
        driver.setStatus(DriverStatus.REJECTED);
        driver.setStatusReason(reason);
        driver.setReviewedAt(LocalDateTime.now(clock));
        return driver;
    }

    @Transactional
    public DriverResponse rejectResponse(@NotNull Long driverId, @NotBlank String reason) {
        return DriverResponse.from(reject(driverId, reason));
    }

    @Transactional
    public Driver resubmitForReview(@NotNull Long driverId) {
        Driver driver = findById(driverId);
        requireStatus(driver, DriverStatus.REJECTED);
        driver.setStatus(DriverStatus.PENDING);
        driver.setStatusReason(null);
        driver.setReviewedAt(null);
        return driver;
    }

    @Transactional
    public DriverResponse resubmitForReviewResponse(@NotNull Long driverId) {
        return DriverResponse.from(resubmitForReview(driverId));
    }

    @Transactional
    public Driver suspend(@NotNull Long driverId, @NotBlank String reason) {
        Driver driver = findById(driverId);
        requireStatus(driver, DriverStatus.APPROVED);
        driver.setStatus(DriverStatus.SUSPENDED);
        driver.setStatusReason(reason);
        driver.setReviewedAt(LocalDateTime.now(clock));

        LocalDateTime revokedAt = LocalDateTime.now(clock);
        for (DriverInvite invite : inviteRepository.findAllByDriverIdAndStatus(driverId, InviteStatus.ACTIVE)) {
            invite.setStatus(InviteStatus.REVOKED);
            invite.setRevokedAt(revokedAt);
        }
        return driver;
    }

    @Transactional
    public DriverResponse suspendResponse(@NotNull Long driverId, @NotBlank String reason) {
        return DriverResponse.from(suspend(driverId, reason));
    }

    @Transactional
    public Driver reactivate(@NotNull Long driverId) {
        Driver driver = findById(driverId);
        requireStatus(driver, DriverStatus.SUSPENDED);
        driver.setStatus(DriverStatus.APPROVED);
        driver.setStatusReason(null);
        driver.setReviewedAt(LocalDateTime.now(clock));
        return driver;
    }

    @Transactional
    public DriverResponse reactivateResponse(@NotNull Long driverId) {
        return DriverResponse.from(reactivate(driverId));
    }

    @Transactional(readOnly = true)
    public Driver requireApproved(@NotNull Long driverId) {
        Driver driver = findById(driverId);
        if (driver.getStatus() != DriverStatus.APPROVED) {
            throw new BusinessRuleException("O motorista precisa estar aprovado para executar esta ação");
        }
        return driver;
    }

    @Transactional(readOnly = true)
    public Driver requireOperationalView(@NotNull Long driverId) {
        Driver driver = findById(driverId);
        if (driver.getStatus() != DriverStatus.APPROVED
                && driver.getStatus() != DriverStatus.SUSPENDED) {
            throw new BusinessRuleException("O motorista com este status pode visualizar apenas a própria análise");
        }
        return driver;
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> findAll(DriverStatus status) {
        List<Driver> drivers = status == null
                ? driverRepository.findAll()
                : driverRepository.findAllByStatus(status);
        return drivers.stream().map(DriverResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DriverResponse getDetails(@NotNull Long driverId) {
        return DriverResponse.from(findById(driverId));
    }

    @Transactional
    public DriverResponse setOperationalAddress(
            @NotNull Long driverId, @Valid AddressRequest request) {
        Driver driver = requireApproved(driverId);
        Address address = driver.getOperationalAddress();
        if (address == null || address == driver.getUser().getAddress()) {
            address = addressRepository.save(newAddress(request));
            driver.setOperationalAddress(address);
        } else {
            copyAddress(address, request);
        }
        return DriverResponse.from(driver);
    }

    @Transactional
    public DriverResponse useRegistrationAddressForOperation(@NotNull Long driverId) {
        Driver driver = requireApproved(driverId);
        Address previous = driver.getOperationalAddress();
        driver.setOperationalAddress(null);
        if (previous != null && previous != driver.getUser().getAddress()) {
            addressRepository.delete(previous);
        }
        return DriverResponse.from(driver);
    }

    private void requireStatus(Driver driver, DriverStatus expected) {
        if (driver.getStatus() != expected) {
            throw new BusinessRuleException(
                    "Transição inválida: o motorista precisa estar com status " + expected);
        }
    }

    private void copyAddress(Address target, AddressRequest source) {
        target.setStreet(source.street().trim());
        target.setNumber(source.number().trim());
        target.setComplement(source.complement());
        target.setNeighborhood(source.neighborhood().trim());
        target.setCity(source.city().trim());
        target.setState(source.state().trim().toUpperCase());
        target.setZipCode(source.zipCode().trim());
        target.setLatitude(source.latitude());
        target.setLongitude(source.longitude());
    }

    private Address newAddress(AddressRequest source) {
        Address address = new Address();
        copyAddress(address, source);
        return address;
    }
}
