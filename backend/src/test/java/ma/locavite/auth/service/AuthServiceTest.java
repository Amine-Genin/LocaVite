package ma.locavite.auth.service;

import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.dto.*;
import ma.locavite.auth.exception.EmailAlreadyExistsException;
import ma.locavite.auth.exception.InvalidTokenException;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;
    private Role clientRole;
    private User existingUser;

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

        existingUser = User.builder()
                .id(1L)
                .nom("Benali")
                .prenom("Karim")
                .email("karim.benali@example.com")
                .telephone("+212600112233")
                .password(passwordEncoder.encode("LocaVite2026"))
                .roles(Set.of(clientRole))
                .build();
    }

    @Test
    @DisplayName("register() doit créer l'utilisateur avec rôle CLIENT, hasher le mot de passe et envoyer l'email de confirmation")
    void register_shouldCreateUser() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(clientRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            toSave.setId(10L);
            toSave.setCreatedAt(LocalDateTime.now());
            return toSave;
        });

        UserResponse response = authService.register(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getNom()).isEqualTo("Benali");
        assertThat(response.getPrenom()).isEqualTo("Karim");
        assertThat(response.getEmail()).isEqualTo("karim.benali@example.com");
        assertThat(response.getTelephone()).isEqualTo("+212600112233");
        assertThat(response.getRoles()).contains("CLIENT");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertThat(capturedUser.getEmail()).isEqualTo("karim.benali@example.com");
        assertThat(capturedUser.getPassword()).isNotEqualTo("LocaVite2026");
        assertThat(passwordEncoder.matches("LocaVite2026", capturedUser.getPassword())).isTrue();
        assertThat(capturedUser.getRoles()).extracting(Role::getName).contains("CLIENT");

        verify(emailService, times(1)).sendConfirmationEmail(capturedUser);
    }

    @Test
    @DisplayName("register() doit lever une EmailAlreadyExistsException si l'email existe déjà")
    void register_shouldThrowIfEmailExists() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining(validRequest.getEmail());

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendConfirmationEmail(any(User.class));
    }

    @Test
    @DisplayName("login() doit authentifier, générer accessToken + refreshToken et renvoyer AuthResponse")
    void login_success() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("karim.benali@example.com")
                .password("LocaVite2026")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("karim.benali@example.com")).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(existingUser)).thenReturn("mocked.jwt.token");
        when(jwtService.getJwtExpiration()).thenReturn(86400000L);
        when(refreshTokenService.createRefreshToken("karim.benali@example.com")).thenReturn("mocked-refresh-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getRefreshToken()).isEqualTo("mocked-refresh-token");
        assertThat(response.getExpiresIn()).isEqualTo(86400L);
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("karim.benali@example.com");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(existingUser);
        verify(refreshTokenService).createRefreshToken("karim.benali@example.com");
    }

    @Test
    @DisplayName("login() avec mauvais identifiants doit lever BadCredentialsException")
    void login_badCredentials() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("karim.benali@example.com")
                .password("WrongPassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Email ou mot de passe incorrect");

        verify(jwtService, never()).generateToken(any(User.class));
        verify(refreshTokenService, never()).createRefreshToken(anyString());
    }

    @Test
    @DisplayName("refreshToken() avec token valide doit révoquer l'ancien et renvoyer une nouvelle AuthResponse")
    void refreshToken_success() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        when(refreshTokenService.validateAndGetEmail("valid-refresh-token"))
                .thenReturn(Optional.of("karim.benali@example.com"));
        when(userRepository.findByEmail("karim.benali@example.com"))
                .thenReturn(Optional.of(existingUser));
        when(refreshTokenService.createRefreshToken("karim.benali@example.com"))
                .thenReturn("new-refresh-token");
        when(jwtService.generateToken(existingUser))
                .thenReturn("new.access.token");
        when(jwtService.getJwtExpiration()).thenReturn(86400000L);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenService).revokeRefreshToken("valid-refresh-token");
        verify(refreshTokenService).createRefreshToken("karim.benali@example.com");
    }

    @Test
    @DisplayName("refreshToken() avec token invalide ou expiré doit lever InvalidTokenException")
    void refreshToken_invalidTokenThrows() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-token")
                .build();

        when(refreshTokenService.validateAndGetEmail("invalid-token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("logout() révoque le refresh token via RefreshTokenService")
    void logout_success() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("some-refresh-token")
                .build();

        authService.logout(request);

        verify(refreshTokenService).revokeRefreshToken("some-refresh-token");
    }
}
