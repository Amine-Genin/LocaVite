package ma.locavite.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.dto.RegisterRequest;
import ma.locavite.auth.repository.RoleRepository;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.auth.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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

    @MockBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        doNothing().when(emailService).sendConfirmationEmail(any(User.class));
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
    @DisplayName("POST /api/auth/register - Échec : 400 Bad Request sur mot de passe trop simple (< 8 cars, sans majuscule ou chiffre)")
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
}

