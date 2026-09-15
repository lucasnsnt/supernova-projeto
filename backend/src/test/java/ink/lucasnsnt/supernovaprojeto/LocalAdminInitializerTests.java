package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.config.LocalAdminProperties;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import ink.lucasnsnt.supernovaprojeto.services.LocalAdminInitializer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LocalAdminInitializerTests {
    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final LocalAdminProperties properties = new LocalAdminProperties("Admin@example.test", "local-test-password");
    private final LocalAdminInitializer initializer = new LocalAdminInitializer(repository, encoder, properties);

    @Test
    void createsAdminWithEncodedPassword() {
        when(encoder.encode(properties.password())).thenReturn("encoded-password");
        initializer.run(null);
        var user = ArgumentCaptor.forClass(User.class);
        verify(repository).save(user.capture());
        assertThat(user.getValue().getEmail()).isEqualTo("admin@example.test");
        assertThat(user.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(user.getValue().getEmailVerifiedAt()).isNotNull();
    }

    @Test
    void doesNotOverwriteExistingAccount() {
        when(repository.existsByEmailIgnoreCase("admin@example.test")).thenReturn(true);
        initializer.run(null);
        verify(repository, never()).save(any());
        verifyNoInteractions(encoder);
    }
}
