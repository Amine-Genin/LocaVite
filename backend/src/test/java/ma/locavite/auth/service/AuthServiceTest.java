package ma.locavite.auth.service;

import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.dto.RegisterRequest;
import ma.locavite.auth.dto.UserResponse;
import ma.locavite.auth.exception.EmailAlreadyExistsException;
import ma.locavite.auth.mapper.UserMapper;
import ma.locavite.auth.repository.RoleRepository;
import ma.locavite.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @Spy
    private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;
    private Role clientRole;

    @BeforeEach
    void setUp() {
        validRequest = RegisterRequest.builder()
                .nom("Benali")
                .prenom("Karim")
                .email("karim.benali@example.com")
                .telephone("+212600112233")
                .password("LocaVite2026")
                .build();

        clientRole = Role.builder()
                .id(1L)
                .name("CLIENT")
                .build();
    }

    @Test
    @DisplayName("register() doit créer l'utilisateur avec rôle CLIENT, hasher le mot de passe et envoyer l'email de confirmation")
    void register_shouldCreateUser() {
        // GIVEN
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(clientRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            toSave.setId(10L);
            toSave.setCreatedAt(LocalDateTime.now());
            return toSave;
        });

        // WHEN
        UserResponse response = authService.register(validRequest);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getNom()).isEqualTo("Benali");
        assertThat(response.getPrenom()).isEqualTo("Karim");
        assertThat(response.getEmail()).isEqualTo("karim.benali@example.com");
        assertThat(response.getTelephone()).isEqualTo("+212600112233");
        assertThat(response.getRoles()).contains("CLIENT");

        // Vérification de la persistance de l'entité User
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertThat(capturedUser.getEmail()).isEqualTo("karim.benali@example.com");
        assertThat(capturedUser.getNom()).isEqualTo("Benali");
        assertThat(capturedUser.getPrenom()).isEqualTo("Karim");
        assertThat(capturedUser.getTelephone()).isEqualTo("+212600112233");
        // Vérification que le mot de passe est hashé avec BCrypt (force 12)
        assertThat(capturedUser.getPassword()).isNotEqualTo("LocaVite2026");
        assertThat(passwordEncoder.matches("LocaVite2026", capturedUser.getPassword())).isTrue();
        // Vérification que le rôle CLIENT est attribué
        assertThat(capturedUser.getRoles()).extracting(Role::getName).contains("CLIENT");

        // Vérification de l'envoi de l'email de confirmation
        verify(emailService, times(1)).sendConfirmationEmail(capturedUser);
    }

    @Test
    @DisplayName("register() doit lever une EmailAlreadyExistsException si l'email existe déjà")
    void register_shouldThrowIfEmailExists() {
        // GIVEN
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        // WHEN & THEN
        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining(validRequest.getEmail());

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendConfirmationEmail(any(User.class));
    }
}

