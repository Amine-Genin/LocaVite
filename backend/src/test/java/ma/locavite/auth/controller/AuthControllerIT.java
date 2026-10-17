package ma.locavite.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.dto.LoginRequest;
import ma.locavite.auth.dto.RefreshTokenRequest;
import ma.locavite.auth.dto.RegisterRequest;
import ma.locavite.auth.repository.RoleRepository;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.auth.service.EmailService;
import ma.locavite.auth.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        doNothing().when(emailService).sendConfirmationEmail(any(User.class));
        when(refreshTokenService.createRefreshToken(anyString())).thenReturn("mocked-refresh-token-12345");
    }

    @Test
    @DisplayName("POST /api/auth/register - Succès : renvoie 201 Created et le user sans mot de passe")
    void register() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .nom("Alaoui")
                .prenom("Fatima")
                .email("fatima.alaoui@example.com")
                .telephone("+212611223344")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nom", is("Alaoui")))
                .andExpect(jsonPath("$.prenom", is("Fatima")))
                .andExpect(jsonPath("$.email", is("fatima.alaoui@example.com")))
                .andExpect(jsonPath("$.telephone", is("+212611223344")))
                .andExpect(jsonPath("$.roles", hasItem("CLIENT")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/register - Échec : 400 Bad Request si l'email est déjà utilisé")
    void register_shouldReturn400WhenEmailAlreadyExists() throws Exception {
        RegisterRequest request1 = RegisterRequest.builder()
                .nom("Alaoui")
                .prenom("Fatima")
                .email("fatima.alaoui@example.com")
                .telephone("+212611223344")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        RegisterRequest request2 = RegisterRequest.builder()
                .nom("Benani")
                .prenom("Sara")
                .email("fatima.alaoui@example.com")
                .telephone("+212699887766")
                .password("Password456")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("fatima.alaoui@example.com")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Échec : 400 Bad Request sur email invalide")
    void register_shouldReturn400WhenEmailInvalid() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .nom("Alaoui")
                .prenom("Fatima")
                .email("format-email-invalide")
                .telephone("+212611223344")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.email", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/auth/register - Échec : 400 Bad Request sur mot de passe trop simple")
    void register_shouldReturn400WhenPasswordTooWeak() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .nom("Alaoui")
                .prenom("Fatima")
                .email("fatima.alaoui@example.com")
                .telephone("+212611223344")
                .password("weak")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/auth/login - Succès : 200 OK avec accessToken, refreshToken, expiresIn, user")
    void login_shouldReturn200AndTokensOnValidCredentials() throws Exception {
        Role clientRole = roleRepository.findByName("CLIENT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("CLIENT").build()));

        User user = User.builder()
                .nom("Alaoui")
                .prenom("Fatima")
                .email("fatima.alaoui@example.com")
                .telephone("+212611223344")
                .password(passwordEncoder.encode("Password123"))
                .roles(Set.of(clientRole))
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("fatima.alaoui@example.com")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", is("mocked-refresh-token-12345")))
                .andExpect(jsonPath("$.expiresIn", is(86400)))
                .andExpect(jsonPath("$.user.email", is("fatima.alaoui@example.com")))
                .andExpect(jsonPath("$.user.nom", is("Alaoui")))
                .andExpect(jsonPath("$.user.roles", hasItem("CLIENT")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Échec : 401 Unauthorized sur mot de passe invalide")
    void login_shouldReturn401WhenPasswordIsIncorrect() throws Exception {
        Role clientRole = roleRepository.findByName("CLIENT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("CLIENT").build()));

        User user = User.builder()
                .nom("Alaoui")
                .prenom("Fatima")
                .email("fatima.alaoui@example.com")
                .telephone("+212611223344")
                .password(passwordEncoder.encode("Password123"))
                .roles(Set.of(clientRole))
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("fatima.alaoui@example.com")
                .password("MauvaisPassword999")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", containsString("Email ou mot de passe incorrect")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Échec : 401 Unauthorized si utilisateur inexistant")
    void login_shouldReturn401WhenUserNotFound() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("inconnu@example.com")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Succès : rafraîchit le token et renvoie 200 OK")
    void refresh_shouldReturnNewTokensOnValidRefreshToken() throws Exception {
        Role clientRole = roleRepository.findByName("CLIENT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("CLIENT").build()));

        User user = User.builder()
                .nom("Alaoui")
                .prenom("Fatima")
                .email("fatima.alaoui@example.com")
                .telephone("+212611223344")
                .password(passwordEncoder.encode("Password123"))
                .roles(Set.of(clientRole))
                .build();
        userRepository.save(user);

        when(refreshTokenService.validateAndGetEmail("valid-sample-refresh-token"))
                .thenReturn(Optional.of("fatima.alaoui@example.com"));
        when(refreshTokenService.createRefreshToken("fatima.alaoui@example.com"))
                .thenReturn("new-refreshed-token-99999");

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-sample-refresh-token")
                .build();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", is("new-refreshed-token-99999")))
                .andExpect(jsonPath("$.user.email", is("fatima.alaoui@example.com")));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Échec : 401 Unauthorized si refresh token invalide")
    void refresh_shouldReturn401WhenRefreshTokenIsInvalid() throws Exception {
        when(refreshTokenService.validateAndGetEmail("invalid-sample-refresh-token"))
                .thenReturn(Optional.empty());

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-sample-refresh-token")
                .build();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("POST /api/auth/logout - Succès : 204 No Content et révocation du refresh token")
    void logout_shouldReturn204() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("sample-refresh-token-to-revoke")
                .build();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}
