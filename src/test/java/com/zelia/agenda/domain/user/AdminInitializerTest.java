package com.zelia.agenda.domain.user;

import com.zelia.agenda.exceptions.InvalidPasswordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new AdminInitializer(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "adminEmail", "admin@exemplo.com");
        ReflectionTestUtils.setField(initializer, "adminPassword", "SenhaForte#2026");
    }

    @Test
    void createsAdminWhenNotExists() {
        when(userRepository.findByEmailIgnoreCase("admin@exemplo.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("SenhaForte#2026")).thenReturn("hash-admin");

        initializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User admin = captor.getValue();
        assertThat(admin.getName()).isEqualTo("Administrador");
        assertThat(admin.getEmail()).isEqualTo("admin@exemplo.com");
        assertThat(admin.getUserType()).isEqualTo(UserType.ADMIN);
        assertThat(admin.getActive()).isTrue();
        assertThat(admin.getPassword()).isEqualTo("hash-admin");
    }

    @Test
    void doesNotCreateAdminWithWeakPassword() {
        ReflectionTestUtils.setField(initializer, "adminPassword", "curta");
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> initializer.run())
                .isInstanceOf(InvalidPasswordException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void doesNotChangeExistingAdmin() {
        User existente = new User("Administrador", "admin@exemplo.com", "hash-antigo", UserType.ADMIN, true);
        when(userRepository.findByEmailIgnoreCase("admin@exemplo.com")).thenReturn(Optional.of(existente));
        when(passwordEncoder.matches("admin", "hash-antigo")).thenReturn(false);

        initializer.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void legacyDefaultPasswordOnlyLogsError() {
        User existente = new User("Administrador", "admin@exemplo.com", "hash-antigo", UserType.ADMIN, true);
        when(userRepository.findByEmailIgnoreCase("admin@exemplo.com")).thenReturn(Optional.of(existente));
        when(passwordEncoder.matches("admin", "hash-antigo")).thenReturn(true);

        initializer.run();

        verify(userRepository, never()).save(any());
    }
}
