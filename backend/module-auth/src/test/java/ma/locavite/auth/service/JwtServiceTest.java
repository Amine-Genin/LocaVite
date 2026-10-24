package ma.locavite.auth.service;

import io.jsonwebtoken.Claims;
import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970337336763979244226452948404D6351655468576D5A7134743777217A25432A";
    private static final long EXPIRATION_MS = 86400000L; // 24 hours

    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);

        Role roleClient = Role.builder().id(1L).name("CLIENT").build();
        testUser = User.builder()
                .id(1L)
                .nom("Benali")
                .prenom("Karim")
                .email("karim.benali@example.com")
                .password("encoded_password")
                .roles(Set.of(roleClient))
                .build();
    }

    @Test
    @DisplayName("generateToken() doit générer un token JWT contenant sub, roles, iat et exp")
    void generateToken() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);

        Claims claims = jwtService.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo("karim.benali@example.com");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(new Date());

        List<String> roles = jwtService.extractRoles(token);
        assertThat(roles).containsExactly("CLIENT");
    }

    @Test
    @DisplayName("validateToken() doit valider un token valide pour le bon utilisateur")
    void validateToken() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, testUser);
        assertThat(isValid).isTrue();

        boolean isValidByEmail = jwtService.isTokenValid(token, "karim.benali@example.com");
        assertThat(isValidByEmail).isTrue();

        boolean isInvalidForOtherUser = jwtService.isTokenValid(token, "autre@example.com");
        assertThat(isInvalidForOtherUser).isFalse();
    }

    @Test
    @DisplayName("validateToken() doit retourner false pour un token expiré")
    void tokenExpiredReturnsFalse() {
        JwtService shortLivedJwtService = new JwtService(SECRET, -1000L); // déjà expiré
        String expiredToken = shortLivedJwtService.generateToken(testUser);

        boolean isValid = shortLivedJwtService.isTokenValid(expiredToken, testUser);
        assertThat(isValid).isFalse();
        assertThat(shortLivedJwtService.isTokenExpired(expiredToken)).isTrue();
    }

    @Test
    @DisplayName("validateToken() doit retourner false pour un token altéré")
    void tamperedTokenReturnsFalse() {
        String token = jwtService.generateToken(testUser);
        String tamperedToken = token + "xyz";

        boolean isValid = jwtService.isTokenValid(tamperedToken, testUser);
        assertThat(isValid).isFalse();
    }
}
