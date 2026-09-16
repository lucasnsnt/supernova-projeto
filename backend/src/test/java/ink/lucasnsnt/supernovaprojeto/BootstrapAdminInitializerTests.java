package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.config.BootstrapAdminProperties;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import ink.lucasnsnt.supernovaprojeto.services.BootstrapAdminInitializer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BootstrapAdminInitializerTests {
    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final BootstrapAdminProperties properties =
            new BootstrapAdminProperties("Yasmini@Admin", "yasmini123123", "Yasmini");
    private final BootstrapAdminInitializer initializer =
            new BootstrapAdminInitializer(repository, encoder, properties);

    @Test
    void createsOneAdminAndEncodesTheTemporaryPassword() {
        when(encoder.encode("yasmini123123")).thenReturn("encoded");
        initializer.run(null);
        var captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("yasmini@admin");
        assertThat(captor.getValue().getName()).isEqualTo("Yasmini");
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void neverOverwritesAnExistingAccount() {
        when(repository.existsByEmailIgnoreCase("yasmini@admin")).thenReturn(true);
        initializer.run(null);
        verify(repository, never()).save(any());
        verifyNoInteractions(encoder);
    }
}
