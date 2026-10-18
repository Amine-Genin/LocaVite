package ma.locavite.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.RoleType;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.RoleRepository;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.auth.service.EmailService;
import ma.locavite.auth.service.JwtService;
import ma.locavite.auth.service.RefreshTokenService;
import ma.locavite.user.dto.AssignRolesRequest;
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

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUserControllerIT {

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

    @Autowired
    private JwtService jwtService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    private Role roleAdmin;
    private Role roleClient;
    private Role roleAgence;

    private User adminUser;
    private User clientUser;
    private String adminToken;
    private String clientToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        roleAdmin = roleRepository.findByName(RoleType.ADMIN.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ADMIN.name()).build()));
        roleClient = roleRepository.findByName(RoleType.CLIENT.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.CLIENT.name()).build()));
        roleAgence = roleRepository.findByName(RoleType.AGENCE.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.AGENCE.name()).build()));

        adminUser = User.builder()
                .nom("AdminNom")
                .prenom("AdminPrenom")
                .email("admin@locavite.ma")
                .telephone("+212600112233")
                .password(passwordEncoder.encode("Password123!"))
                .roles(Set.of(roleAdmin))
                .build();
        adminUser = userRepository.save(adminUser);

        clientUser = User.builder()
                .nom("ClientNom")
                .prenom("ClientPrenom")
                .email("client@locavite.ma")
                .telephone("+212699887766")
                .password(passwordEncoder.encode("Password123!"))
                .roles(Set.of(roleClient))
                .build();
        clientUser = userRepository.save(clientUser);

        adminToken = jwtService.generateToken(adminUser);
        clientToken = jwtService.generateToken(clientUser);
    }

    @Test
    @DisplayName("GET /api/admin/users - Succès : 200 OK avec liste paginée de DTO sans mot de passe pour ADMIN")
    void getUsers_asAdmin_shouldReturn200AndPagedUsersWithoutPasswords() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[1].password").doesNotExist())
                .andExpect(jsonPath("$.content[*].email", hasItems("admin@locavite.ma", "client@locavite.ma")));
    }

    @Test
    @DisplayName("GET /api/admin/users - Échec : 403 Forbidden pour CLIENT")
    void getUsers_asClient_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("GET /api/admin/users - Échec : 401 Unauthorized sans token")
    void getUsers_withoutToken_shouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/roles - Succès : 200 OK lorsque ADMIN assigne de nouveaux rôles")
    void assignRole_asAdmin_shouldReturn200AndUpdatedRoles() throws Exception {
        AssignRolesRequest request = AssignRolesRequest.builder()
                .roles(Set.of("CLIENT", "AGENCE"))
                .build();

        mockMvc.perform(put("/api/admin/users/" + clientUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(clientUser.getId().intValue())))
                .andExpect(jsonPath("$.email", is("client@locavite.ma")))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("CLIENT", "AGENCE")))
                .andExpect(jsonPath("$.password").doesNotExist());

        // Vérification de la persistance en base de données
        User updatedUser = userRepository.findById(clientUser.getId()).orElseThrow();
        assertTrue(updatedUser.getRoles().stream().anyMatch(r -> "CLIENT".equals(r.getName())));
        assertTrue(updatedUser.getRoles().stream().anyMatch(r -> "AGENCE".equals(r.getName())));
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/roles - Échec : 403 Forbidden lorsque CLIENT tente d'assigner un rôle")
    void assignRole_asClient_shouldReturn403Forbidden() throws Exception {
        AssignRolesRequest request = AssignRolesRequest.builder()
                .roles(Set.of("ADMIN"))
                .build();

        mockMvc.perform(put("/api/admin/users/" + clientUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/roles - Échec : 401 Unauthorized sans token")
    void assignRole_withoutToken_shouldReturn401Unauthorized() throws Exception {
        AssignRolesRequest request = AssignRolesRequest.builder()
                .roles(Set.of("AGENCE"))
                .build();

        mockMvc.perform(put("/api/admin/users/" + clientUser.getId() + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/roles - Échec : 400 Bad Request si le rôle est inexistant")
    void assignRole_withInvalidRole_shouldReturn400BadRequest() throws Exception {
        AssignRolesRequest request = AssignRolesRequest.builder()
                .roles(Set.of("SUPER_ADMIN_INCONNU"))
                .build();

        mockMvc.perform(put("/api/admin/users/" + clientUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Rôle inexistant")));
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/roles - Échec : 404 Not Found si l'utilisateur est inexistant")
    void assignRole_withNonExistingUser_shouldReturn404NotFound() throws Exception {
        AssignRolesRequest request = AssignRolesRequest.builder()
                .roles(Set.of("CLIENT"))
                .build();

        mockMvc.perform(put("/api/admin/users/999999/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Utilisateur non trouvé")));
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/roles - Échec : 400 Bad Request si l'admin tente de se retirer lui-même le rôle ADMIN")
    void assignRole_adminDemotingThemselves_shouldReturn400BadRequest() throws Exception {
        AssignRolesRequest request = AssignRolesRequest.builder()
                .roles(Set.of("CLIENT", "AGENCE"))
                .build();

        mockMvc.perform(put("/api/admin/users/" + adminUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Un administrateur ne peut pas se retirer lui-même le rôle ADMIN")));
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/roles - Échec : 400 Bad Request sur validation @Valid si la liste est vide")
    void assignRole_withEmptyRolesList_shouldReturn400BadRequest() throws Exception {
        AssignRolesRequest request = AssignRolesRequest.builder()
                .roles(Set.of())
                .build();

        mockMvc.perform(put("/api/admin/users/" + clientUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.roles", notNullValue()));
    }
}
