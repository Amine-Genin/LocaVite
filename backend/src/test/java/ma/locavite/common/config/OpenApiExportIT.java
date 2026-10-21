package ma.locavite.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiExportIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Génère et exporte automatiquement docs/openapi.json à chaque build")
    void exportOpenApiSpecification() throws Exception {
        String openApiJson = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("LocaVite API"))
                .andExpect(jsonPath("$.components.securitySchemes.BearerAuth").exists())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertNotNull(openApiJson);
        assertTrue(openApiJson.contains("openapi"));

        // Formatage JSON indenté pour une meilleure lisibilité
        Object jsonObject = objectMapper.readValue(openApiJson, Object.class);
        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

        // Détection du répertoire docs (soit ../docs depuis backend, soit docs à la racine)
        Path docsDir = Paths.get("..", "docs");
        if (!Files.exists(docsDir)) {
            docsDir = Paths.get("docs");
        }
        if (!Files.exists(docsDir)) {
            Files.createDirectories(docsDir);
        }

        Path targetFile = docsDir.resolve("openapi.json");
        Files.writeString(targetFile, prettyJson, StandardCharsets.UTF_8);
        assertTrue(Files.exists(targetFile));
    }

    @Test
    @DisplayName("Vérifie l'accès aux groupes OpenAPI (Auth, Users, Documents, Vehicles, Reservations)")
    void checkOpenApiGroups() throws Exception {
        mockMvc.perform(get("/v3/api-docs/Auth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());

        mockMvc.perform(get("/v3/api-docs/Users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());

        mockMvc.perform(get("/v3/api-docs/Documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());

        mockMvc.perform(get("/v3/api-docs/Vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());

        mockMvc.perform(get("/v3/api-docs/Reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());
    }
}

