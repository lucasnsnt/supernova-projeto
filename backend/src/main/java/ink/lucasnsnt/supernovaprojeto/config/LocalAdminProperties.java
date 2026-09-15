package ink.lucasnsnt.supernovaprojeto.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.local-admin")
public record LocalAdminProperties(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password) {
}
