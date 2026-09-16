package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.config.BootstrapAdminProperties;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
@EnableConfigurationProperties(BootstrapAdminProperties.class)
public class BootstrapAdminInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.info("Bootstrap administrativo ignorado: a conta já existe");
            return;
        }
        userRepository.save(User.builder()
                .name(properties.name().trim())
                .email(email)
                .password(passwordEncoder.encode(properties.password()))
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .emailVerifiedAt(LocalDateTime.now())
                .role(Role.ADMIN)
                .build());
        log.warn("Conta administrativa inicial criada; remova as variáveis ADMIN_BOOTSTRAP_* do ambiente");
    }
}
