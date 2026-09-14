package ink.lucasnsnt.supernovaprojeto.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    @NotBlank
    private String issuer = "supernova-api";
    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofDays(30);
    private Duration verificationCodeTtl = Duration.ofMinutes(10);
    private Duration registrationTokenTtl = Duration.ofMinutes(15);
    private Duration verificationResendCooldown = Duration.ofMinutes(1);
    private Duration pendingRegistrationRetention = Duration.ofHours(24);
    @Positive
    private int maxVerificationAttempts = 5;
    private String jwtSecret;
    private String refreshCookieName = "supernova_refresh";
    private boolean secureCookies = false;
    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));
}
