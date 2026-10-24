package ma.locavite.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "LocaVite API",
                version = "1.0.0",
                description = "Documentation interactive de l'API REST LocaVite (plateforme de location et vente de véhicules au Maroc). Utilisez le bouton 'Authorize' avec votre JWT (format : Bearer <token>) pour tester les endpoints sécurisés.",
                contact = @Contact(
                        name = "Équipe LocaVite",
                        email = "contact@locavite.ma"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        security = @SecurityRequirement(name = "BearerAuth")
)
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Authentification JWT. Entrez votre token d'accès généré via l'endpoint /api/auth/login"
)
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("Auth")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi usersApi() {
        return GroupedOpenApi.builder()
                .group("Users")
                .pathsToMatch("/api/users/**", "/api/admin/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi documentsApi() {
        return GroupedOpenApi.builder()
                .group("Documents")
                .pathsToMatch("/api/users/me/documents/**", "/api/admin/documents/**")
                .build();
    }

    @Bean
    public GroupedOpenApi vehiclesApi() {
        return GroupedOpenApi.builder()
                .group("Vehicles")
                .pathsToMatch("/api/vehicles/**")
                .build();
    }

    @Bean
    public GroupedOpenApi reservationsApi() {
        return GroupedOpenApi.builder()
                .group("Reservations")
                .pathsToMatch("/api/reservations/**")
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("All")
                .pathsToMatch("/api/**")
                .build();
    }
}
