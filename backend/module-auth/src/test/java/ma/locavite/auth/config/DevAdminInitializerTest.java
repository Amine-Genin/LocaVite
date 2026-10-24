package ma.locavite.auth.config;

import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.RoleType;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.RoleRepository;
import ma.locavite.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevAdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DevAdminInitializer devAdminInitializer;

    @Test
    @DisplayName("DevAdminInitializer doit créer le compte admin s'il n'existe pas")
    void shouldCreateAdminAccountWhenNotExists() {
        when(userRepository.existsByEmail(DevAdminInitializer.DEV_ADMIN_EMAIL)).thenReturn(false);
        when(roleRepository.findByName(RoleType.ADMIN.name())).thenReturn(Optional.of(Role.builder().name("ADMIN").build()));
        when(passwordEncoder.encode(DevAdminInitializer.DEV_ADMIN_PASSWORD)).thenReturn("$2a$12$hashedPassword");

        devAdminInitializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User created = userCaptor.getValue();

        assertEquals(DevAdminInitializer.DEV_ADMIN_EMAIL, created.getEmail());
        assertEquals("$2a$12$hashedPassword", created.getPassword());
        assertTrue(created.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName())));
    }

    @Test
    @DisplayName("DevAdminInitializer ne doit rien faire si l'admin existe déjà")
    void shouldDoNothingWhenAdminAlreadyExists() {
        when(userRepository.existsByEmail(DevAdminInitializer.DEV_ADMIN_EMAIL)).thenReturn(true);

        devAdminInitializer.run();

        verify(userRepository, never()).save(any());
    }
}

