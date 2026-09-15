package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.config.LocalAdminProperties;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Component
@Profile("local & !prod")
@ConditionalOnProperty(name = "app.local-admin.email")
@EnableConfigurationProperties(LocalAdminProperties.class)
public class LocalAdminInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LocalAdminProperties properties;

    public LocalAdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                 LocalAdminProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        userRepository.save(User.builder()
                .name("Administrador local")
                .email(email)
                .password(passwordEncoder.encode(properties.password()))
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .emailVerifiedAt(LocalDateTime.now())
                .role(Role.ADMIN)
                .build());
    }
}
