package ma.locavite.vehicle;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.locavite.vehicle.domain.*;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Nécessite la base de test (et Redis/MinIO si ton contexte les démarre) :
 * lance `docker compose up -d` avant, ou utilise Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VehicleControllerIT {

    private static final Long ME = 1L;
    private static final Long OTHER = 99L;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired VehicleRepository repository;
    @MockBean CurrentUserService currentUser;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        when(currentUser.getCurrentUserId()).thenReturn(Optional.of(ME));
        when(currentUser.isAdmin()).thenReturn(false);
    }

    private Map<String, Object> body(String matricule) {
        return Map.ofEntries(
                Map.entry("marque", "Toyota"), Map.entry("modele", "Corolla"),
                Map.entry("annee", 2022), Map.entry("categorie", "BERLINE"),
                Map.entry("ville", "Casablanca"), Map.entry("prixJour", 350),
                Map.entry("prixVente", 180000), Map.entry("carburant", "DIESEL"),
                Map.entry("transmission", "MANUELLE"), Map.entry("places", 5),
                Map.entry("description", "Très bon état"), Map.entry("matricule", matricule));
    }

    private Vehicle save(Long agenceId, String marque, String ville, String matricule, VehicleStatus statut) {
        Vehicle v = new Vehicle();
        v.setMarque(marque); v.setModele("X"); v.setAnnee(2021); v.setCategorie(CategoryType.SUV);
        v.setVille(ville); v.setPrixJour(new BigDecimal("300")); v.setPrixVente(new BigDecimal("150000"));
        v.setCarburant(FuelType.ESSENCE); v.setTransmission(TransmissionType.AUTOMATIQUE);
        v.setPlaces(5); v.setDescription("desc"); v.setMatricule(matricule);
        v.setStatut(statut); v.setAgenceId(agenceId);
        return repository.saveAndFlush(v);
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    void post_asAgence_returns201WithLocation() throws Exception {
        mvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body("1111-A-1"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.statut").value("DRAFT"))
                .andExpect(jsonPath("$.agenceId").value(ME));
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    void post_duplicateMatricule_returns409() throws Exception {
        save(ME, "Toyota", "Casablanca", "1111-A-1", VehicleStatus.PUBLISHED);

        mvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body("1111-a-1"))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    void post_invalidYearAndPrice_returns400() throws Exception {
        Map<String, Object> invalid = new HashMap<>(body("2222-A-1"));
        invalid.put("annee", 1980);
        invalid.put("prixJour", 0);

        mvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.annee").exists())
                .andExpect(jsonPath("$.errors.prixJour").exists());
    }

    @Test
    void get_byId_isPublic() throws Exception {
        Vehicle v = save(OTHER, "Dacia", "Rabat", "3333-A-1", VehicleStatus.PUBLISHED);

        mvc.perform(get("/api/vehicles/" + v.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marque").value("Dacia"));
    }

    @Test
    void get_list_isPublic_paginatedAndFiltered() throws Exception {
        save(OTHER, "Toyota", "Casablanca", "4444-A-1", VehicleStatus.PUBLISHED);
        save(OTHER, "Toyota", "Casablanca", "4444-A-2", VehicleStatus.DRAFT);     // non public
        save(OTHER, "Dacia", "Rabat", "4444-A-3", VehicleStatus.PUBLISHED);

        mvc.perform(get("/api/vehicles")
                        .param("marque", "toyota").param("categorie", "SUV").param("ville", "Casablanca")
                        .param("page", "0").param("size", "20").param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].matricule").value("4444-A-1"));
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    void put_asOwner_returns200() throws Exception {
        Vehicle v = save(ME, "Toyota", "Casablanca", "5555-A-1", VehicleStatus.PUBLISHED);

        mvc.perform(put("/api/vehicles/" + v.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body("5555-A-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modele").value("Corolla"));
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    void putAndDelete_onOtherAgencyVehicle_return403() throws Exception {
        Vehicle v = save(OTHER, "Toyota", "Casablanca", "6666-A-1", VehicleStatus.PUBLISHED);

        mvc.perform(put("/api/vehicles/" + v.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body("6666-A-1"))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/vehicles/" + v.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    void delete_asOwner_softDeletes() throws Exception {
        Vehicle v = save(ME, "Toyota", "Casablanca", "7777-A-1", VehicleStatus.PUBLISHED);

        mvc.perform(delete("/api/vehicles/" + v.getId())).andExpect(status().isNoContent());

        assertThat(repository.findById(v.getId()).orElseThrow().getStatut()).isEqualTo(VehicleStatus.ARCHIVED);
        mvc.perform(get("/api/vehicles/" + v.getId())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_onAnyVehicle_returns204() throws Exception {
        when(currentUser.isAdmin()).thenReturn(true);
        Vehicle v = save(OTHER, "Toyota", "Casablanca", "8888-A-1", VehicleStatus.PUBLISHED);

        mvc.perform(delete("/api/vehicles/" + v.getId())).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void post_asClient_returns403() throws Exception {
        mvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body("9999-A-1"))))
                .andExpect(status().isForbidden());
    }
}