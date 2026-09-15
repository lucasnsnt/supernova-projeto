package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.dtos.auth.AddressRegistrationRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.auth.AuthResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.auth.RegisterRequest;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.models.Address;
import ink.lucasnsnt.supernovaprojeto.models.Student;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import ink.lucasnsnt.supernovaprojeto.security.AuthenticatedUser;
import ink.lucasnsnt.supernovaprojeto.security.IssuedTokens;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final DriverService driverService;
    private final StudentService studentService;
    private final DriverStudentLinkService linkService;
    private final SessionService sessionService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional
    public IssuedTokens register(@Valid RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new BusinessRuleException("Contas administrativas não podem ser criadas publicamente");
        }
        if (request.role() == Role.DRIVER
                && Period.between(request.dateOfBirth(), LocalDate.now(clock)).getYears() < 18) {
            throw new BusinessRuleException("O motorista precisa ter pelo menos 18 anos");
        }

        String email = emailVerificationService.normalize(request.email());
        var verifiedAt = emailVerificationService.consumeRegistrationAuthorization(
                email, request.registrationToken());
        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone().trim())
                .dateOfBirth(request.dateOfBirth())
                .emailVerifiedAt(verifiedAt)
                .role(request.role())
                .address(toAddress(request.address()))
                .build();
        userRepository.save(user);

        if (request.role() == Role.DRIVER) {
            driverService.register(user.getId(), request.cnh().trim());
        } else {
            Student student = studentService.register(user.getId());
            if (request.driverInviteToken() != null && !request.driverInviteToken().isBlank()) {
                linkService.acceptInvite(student.getId(), request.driverInviteToken().trim());
            }
        }
        return sessionService.start(user);
    }

    @Transactional
    public IssuedTokens login(
            @NotBlank @Email String email,
            @NotBlank String password) {
        String normalizedEmail = emailVerificationService.normalize(email);
        AuthenticatedUser principal = (AuthenticatedUser) authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, password))
                .getPrincipal();
        User user = userRepository.findById(principal.id()).orElseThrow();
        return sessionService.start(user);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public IssuedTokens refresh(String refreshToken) {
        return sessionService.rotate(refreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        sessionService.logout(refreshToken);
    }

    @Transactional(readOnly = true)
    public AuthResponse toResponse(IssuedTokens tokens) {
        User user = tokens.user();
        DriverStatus driverStatus = user.getDriver() == null ? null : user.getDriver().getStatus();
        boolean profileComplete = user.getStudent() != null && user.getStudent().isProfileComplete();
        return new AuthResponse(
                tokens.accessToken(), "Bearer", tokens.accessTokenExpiresAt(),
                user.getId(), user.getEmail(), user.getRole(), driverStatus, profileComplete);
    }

    private Address toAddress(AddressRegistrationRequest address) {
        return Address.builder()
                .street(address.street().trim())
                .number(address.number().trim())
                .complement(address.complement())
                .neighborhood(address.neighborhood().trim())
                .city(address.city().trim())
                .state(address.state().trim().toUpperCase())
                .zipCode(address.zipCode().trim())
                .latitude(address.latitude())
                .longitude(address.longitude())
                .build();
    }
}
