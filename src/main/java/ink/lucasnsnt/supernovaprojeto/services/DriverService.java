package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceConflictException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.models.Driver;
import ink.lucasnsnt.supernovaprojeto.models.DriverInvite;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.InviteStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;
import ink.lucasnsnt.supernovaprojeto.repositories.DriverInviteRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.DriverRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Validated
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final DriverInviteRepository inviteRepository;
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
    public Driver reject(@NotNull Long driverId, @NotBlank String reason) {
        Driver driver = findById(driverId);
        requireStatus(driver, DriverStatus.PENDING);
        driver.setStatus(DriverStatus.REJECTED);
        driver.setStatusReason(reason);
        driver.setReviewedAt(LocalDateTime.now(clock));
        return driver;
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
    public Driver suspend(@NotNull Long driverId, String reason) {
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
    public Driver reactivate(@NotNull Long driverId) {
        Driver driver = findById(driverId);
        requireStatus(driver, DriverStatus.SUSPENDED);
        driver.setStatus(DriverStatus.APPROVED);
        driver.setStatusReason(null);
        driver.setReviewedAt(LocalDateTime.now(clock));
        return driver;
    }

    @Transactional(readOnly = true)
    public Driver requireApproved(@NotNull Long driverId) {
        Driver driver = findById(driverId);
        if (driver.getStatus() != DriverStatus.APPROVED) {
            throw new BusinessRuleException("O motorista precisa estar aprovado para executar esta ação");
        }
        return driver;
    }

    private void requireStatus(Driver driver, DriverStatus expected) {
        if (driver.getStatus() != expected) {
            throw new BusinessRuleException(
                    "Transição inválida: o motorista precisa estar com status " + expected);
        }
    }
}
