package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.dtos.auth.AddressRegistrationRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.auth.RegisterRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.auth.RegistrationAuthorizationResponse;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.models.RefreshToken;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.models.enums.RefreshTokenStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;
import ink.lucasnsnt.supernovaprojeto.repositories.EmailVerificationRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.RefreshTokenRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.StudentRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import ink.lucasnsnt.supernovaprojeto.security.IssuedTokens;
import ink.lucasnsnt.supernovaprojeto.security.TokenHasher;
import ink.lucasnsnt.supernovaprojeto.services.AuthService;
import ink.lucasnsnt.supernovaprojeto.services.EmailVerificationService;
import ink.lucasnsnt.supernovaprojeto.services.mail.EmailVerificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
class SecurityFlowTests {

    @Autowired private EmailVerificationService verificationService;
    @Autowired private AuthService authService;
    @Autowired private EmailVerificationRepository verificationRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TokenHasher tokenHasher;

    @MockitoBean
    private EmailVerificationSender emailSender;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        verificationRepository.deleteAll();
        reset(emailSender);
    }

    @Test
    void shouldVerifyEmailRegisterWithHashedPasswordAndRotateSingleSession() {
        String email = "student@security.test";
        verificationService.requestCode(email);

        var codeCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendVerificationCode(eq(email), codeCaptor.capture());
        String code = codeCaptor.getValue();
        assertThat(code).matches("\\d{6}");

        RegistrationAuthorizationResponse authorization =
                verificationService.confirmCode(email, code);
        RegisterRequest request = new RegisterRequest(
                authorization.registrationToken(),
                "Aluno Seguro",
                email,
                "Senha@123",
                "71999999999",
                LocalDate.of(2000, 1, 1),
                Role.STUDENT,
                new AddressRegistrationRequest(
                        "Rua Segura", "10", null, "Centro", "Salvador", "BA", "40000-000"),
                null,
                null);

        IssuedTokens registrationSession = authService.register(request);
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();

        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(studentRepository.existsById(user.getId())).isTrue();
        assertThat(user.getPassword()).isNotEqualTo(request.password());
        assertThat(passwordEncoder.matches(request.password(), user.getPassword())).isTrue();
        assertThat(registrationSession.accessToken()).isNotBlank();
        assertThat(refreshTokenRepository.findAll())
                .noneMatch(token -> token.getTokenHash().equals(registrationSession.refreshToken()));

        IssuedTokens loginSession = authService.login(email.toUpperCase(), request.password());
        RefreshToken firstSession = refreshTokenRepository.findAll().stream()
                .filter(token -> token.getTokenHash().equals(
                        tokenHasher.hash(registrationSession.refreshToken())))
                .findFirst()
                .orElseThrow();
        assertThat(firstSession.getStatus()).isEqualTo(RefreshTokenStatus.REVOKED);

        IssuedTokens rotatedSession = authService.refresh(loginSession.refreshToken());
        assertThat(rotatedSession.refreshToken()).isNotEqualTo(loginSession.refreshToken());
        assertThat(refreshTokenRepository.findAllByUserIdAndStatus(
                user.getId(), RefreshTokenStatus.ACTIVE)).hasSize(1);

        assertThatThrownBy(() -> authService.refresh(loginSession.refreshToken()))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(refreshTokenRepository.findAllByUserIdAndStatus(
                user.getId(), RefreshTokenStatus.ACTIVE)).isEmpty();
    }

    @Test
    void shouldPersistFailedVerificationAttemptsAndBlockTheCode() {
        String email = "attempts@security.test";
        verificationService.requestCode(email);

        var codeCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendVerificationCode(eq(email), codeCaptor.capture());
        String validCode = codeCaptor.getValue();
        String invalidCode = validCode.equals("999999") ? "000000" : "999999";

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> verificationService.confirmCode(email, invalidCode))
                    .isInstanceOf(BusinessRuleException.class);
        }

        assertThat(verificationRepository.findByEmail(email).orElseThrow().getAttemptCount())
                .isEqualTo(5);
        assertThatThrownBy(() -> verificationService.confirmCode(email, validCode))
                .isInstanceOf(BusinessRuleException.class);
    }
}
