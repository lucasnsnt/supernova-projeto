package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.dtos.account.AccountResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.driver.DriverProfileUpdateRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.student.StudentProfileUpdateRequest;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceConflictException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.models.Address;
import ink.lucasnsnt.supernovaprojeto.models.Driver;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;
import ink.lucasnsnt.supernovaprojeto.repositories.DriverRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

@Service
@Validated
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AccountResponse findOwnAccount(@NotNull Long userId) {
        User user = findUser(userId);
        Driver driver = user.getDriver();
        return new AccountResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getDateOfBirth(), user.getRole(), AddressResponse.from(user.getAddress()),
                driver == null ? null : driver.getStatus(),
                driver == null ? null : driver.getStatusReason(),
                driver == null ? null : driver.getCnh());
    }

    @Transactional
    public AccountResponse updatePhone(@NotNull Long userId, @NotBlank String phone) {
        User user = findUser(userId);
        if (user.getRole() == Role.DRIVER) {
            DriverStatus status = user.getDriver().getStatus();
            if (status == DriverStatus.PENDING || status == DriverStatus.SUSPENDED) {
                throw new BusinessRuleException("O motorista com este status pode apenas visualizar os dados");
            }
        }
        user.setPhone(phone.trim());
        return findOwnAccount(userId);
    }

    @Transactional
    public AccountResponse updateStudentProfile(
            @NotNull Long studentId, @Valid StudentProfileUpdateRequest request) {
        User user = requireRole(studentId, Role.STUDENT);
        updateProfile(user, request.name(), request.phone(), request.dateOfBirth(), request.address());
        return findOwnAccount(studentId);
    }

    @Transactional
    public AccountResponse updateRejectedDriverProfile(
            @NotNull Long driverId, @Valid DriverProfileUpdateRequest request) {
        User user = requireRole(driverId, Role.DRIVER);
        Driver driver = user.getDriver();
        if (driver.getStatus() != DriverStatus.REJECTED) {
            throw new BusinessRuleException("Somente um motorista rejeitado pode corrigir estes dados");
        }
        if (Period.between(request.dateOfBirth(), LocalDate.now(clock)).getYears() < 18) {
            throw new BusinessRuleException("O motorista precisa ter pelo menos 18 anos");
        }
        String cnh = request.cnh().trim();
        if (driverRepository.existsByCnhAndIdNot(cnh, driverId)) {
            throw new ResourceConflictException("A CNH já está cadastrada");
        }
        updateProfile(user, request.name(), request.phone(), request.dateOfBirth(), request.address());
        driver.setCnh(cnh);
        return findOwnAccount(driverId);
    }

    @Transactional
    public AccountResponse updateEmail(
            @NotNull Long userId,
            @NotBlank @Email String email,
            @NotBlank String verificationToken) {
        User user = findUser(userId);
        String normalizedEmail = emailVerificationService.normalize(email);
        var verifiedAt = emailVerificationService.consumeRegistrationAuthorization(
                normalizedEmail, verificationToken);
        user.setEmail(normalizedEmail);
        user.setEmailVerifiedAt(verifiedAt);
        return findOwnAccount(userId);
    }

    @Transactional
    public void updatePassword(
            @NotNull Long userId,
            @NotBlank String currentPassword,
            @NotBlank String newPassword) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
    }

    private User requireRole(Long id, Role role) {
        User user = findUser(id);
        if (user.getRole() != role) {
            throw new BusinessRuleException("O perfil não pertence ao papel esperado");
        }
        return user;
    }

    private void updateProfile(
            User user, String name, String phone, LocalDate dateOfBirth, AddressRequest address) {
        user.setName(name.trim());
        user.setPhone(phone.trim());
        user.setDateOfBirth(dateOfBirth);
        copyAddress(user.getAddress(), address);
    }

    private void copyAddress(Address target, AddressRequest source) {
        target.setStreet(source.street().trim());
        target.setNumber(source.number().trim());
        target.setComplement(source.complement());
        target.setNeighborhood(source.neighborhood().trim());
        target.setCity(source.city().trim());
        target.setState(source.state().trim().toUpperCase());
        target.setZipCode(source.zipCode().trim());
        target.setLatitude(null);
        target.setLongitude(null);
    }
}
