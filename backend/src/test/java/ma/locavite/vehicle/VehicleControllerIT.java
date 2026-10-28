
package ma.locavite.vehicle;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.catalog.repository.BrandRepository;
import ma.locavite.catalog.repository.CategoryRepository;
import ma.locavite.vehicle.domain.*;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

@SpringBootTest(properties = "spring.cache.type=none")
@AutoConfigureMockMvc
class VehicleControllerIT {

    private static final Long ME = 1L;
    private static final Long OTHER = 99L;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @Autowired
    VehicleRepository repository;

    @Autowired
    BrandRepository brandRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @MockBean
    CurrentUserService currentUser;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(ME));

        when(currentUser.isAdmin()).thenReturn(false);
    }

    // ---------------- HELPERS ----------------

    private Map<String, Object> body(String matricule) {
        return Map.ofEntries(
                Map.entry("brandId", brandId("Toyota")),
                Map.entry("modele", "Corolla"),
                Map.entry("annee", 2022),
                Map.entry("categoryId", categoryId("BERLINE")),
                Map.entry("ville", "Casablanca"),
                Map.entry("prixJour", 350),
                Map.entry("prixVente", 180000),
                Map.entry("carburant", "DIESEL"),
                Map.entry("transmission", "MANUELLE"),
                Map.entry("places", 5),
                Map.entry("description", "Très bon état"),
                Map.entry("matricule", matricule)
        );
    }

    private Vehicle save(
            Long agenceId,
            String marque,
            String ville,
            String matricule,
            VehicleStatus statut) {

        Vehicle v = new Vehicle();

        v.setBrand(brandRepository.findByNameIgnoreCase(marque).orElseThrow());
        v.setModele("X");
        v.setAnnee(2021);
        v.setCategory(categoryRepository.findByNameIgnoreCase("SUV").orElseThrow());
        v.setVille(ville);
        v.setPrixJour(new BigDecimal("300"));
        v.setPrixVente(new BigDecimal("150000"));
        v.setCarburant(FuelType.ESSENCE);
        v.setTransmission(TransmissionType.AUTOMATIQUE);
        v.setPlaces(5);
        v.setDescription("desc");
        v.setMatricule(matricule);
        v.setStatut(statut);
        v.setAgenceId(agenceId);

        return repository.saveAndFlush(v);
    }

    private Long brandId(String name) {
        return brandRepository.findByNameIgnoreCase(name).orElseThrow().getId();
    }

    private Long categoryId(String name) {
        return categoryRepository.findByNameIgnoreCase(name).orElseThrow().getId();
    }

    // ---------------- CREATE ----------------

    @Test
    @WithMockUser(roles = "AGENCE")
    void post_asAgence_returns201WithLocation() throws Exception {
        mvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body("1111-A-1"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.statut").value("DRAFT"))
                .andExpect(jsonPath("$.agenceId").value(ME));
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    void post_duplicateMatricule_returns409() throws Exception {
        save(ME, "Toyota", "Casablanca",
                "1111-A-1", VehicleStatus.PUBLISHED);

        mvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body("1111-a-1"))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    void post_invalidYearAndPrice_returns400() throws Exception {
        Map<String, Object> invalid =
                new HashMap<>(body("2222-A-1"));

        invalid.put("annee", 1980);
        invalid.put("prixJour", 0);

        mvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.annee").exists())
                .andExpect(jsonPath("$.errors.prixJour").exists());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void post_asClient_returns403() throws Exception {
        mvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body("9999-A-1"))))
                .andExpect(status().isForbidden());
    }

    // ---------------- PUBLIC GET ----------------

    @Test
    void get_byId_isPublic() throws Exception {
        Vehicle v = save(
                OTHER, "Dacia", "Rabat",
                "3333-A-1", VehicleStatus.PUBLISHED);

        mvc.perform(get("/api/vehicles/" + v.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marque").value("Dacia"));
    }

    @Test
    void get_byId_unknown_returns404() throws Exception {
        mvc.perform(get("/api/vehicles/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_byId_draftVehicle_isNotPublic() throws Exception {
        Vehicle v = save(
                OTHER, "Toyota", "Casablanca",
                "3333-A-2", VehicleStatus.DRAFT);

        mvc.perform(get("/api/vehicles/" + v.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_byId_archivedVehicle_returns404() throws Exception {
        Vehicle v = save(
                OTHER, "Toyota", "Casablanca",
                "3333-A-3", VehicleStatus.ARCHIVED);

        mvc.perform(get("/api/vehicles/" + v.getId()))
                .andExpect(status().isNotFound());
    }

    // ---------------- SEARCH AND FILTERS ----------------

    @Test
    void get_list_isPublic_paginatedAndFiltered() throws Exception {
        save(OTHER, "Toyota", "Casablanca",
                "4444-A-1", VehicleStatus.PUBLISHED);

        save(OTHER, "Toyota", "Casablanca",
                "4444-A-2", VehicleStatus.DRAFT);

        save(OTHER, "Dacia", "Rabat",
                "4444-A-3", VehicleStatus.PUBLISHED);

        mvc.perform(get("/api/vehicles")
                        .param("marque", "toyota")
                        .param("categorie", "SUV")
                        .param("ville", "Casablanca")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].brandId").value(brandId("Toyota")))
                .andExpect(jsonPath("$.content[0].categoryId").value(categoryId("SUV")))
                .andExpect(jsonPath("$.content[0].matricule")
                        .value("4444-A-1"));
    }

    @ParameterizedTest
    @CsvSource({
            "marque,Toyota,1",
            "ville,Rabat,1",
            "categorie,SUV,2",
            "marque,Inconnue,0"
    })
    void get_list_singleFilter(
            String param, String value, int expected) throws Exception {

        save(OTHER, "Toyota", "Casablanca",
                "4500-A-1", VehicleStatus.PUBLISHED);

        save(OTHER, "Dacia", "Rabat",
                "4500-A-2", VehicleStatus.PUBLISHED);

        mvc.perform(get("/api/vehicles")
                        .param(param, value))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(expected));
    }

    @Test
    void search_byCity_returnsPublishedVehiclesWithPaginationMetadata() throws Exception {
        Vehicle matching = save(OTHER, "Toyota", "Casablanca",
                "4600-A-1", VehicleStatus.PUBLISHED);
        save(OTHER, "Toyota", "Rabat",
                "4600-A-2", VehicleStatus.PUBLISHED);
        save(OTHER, "Toyota", "Casablanca",
                "4600-A-3", VehicleStatus.DRAFT);

        mvc.perform(get("/api/vehicles/search")
                        .param("ville", "Casablanca")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "prixJour,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(matching.getId()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void search_withCategoryAndDates_appliesFilters() throws Exception {
        Vehicle matching = save(OTHER, "Dacia", "Casablanca",
                "4600-A-4", VehicleStatus.PUBLISHED);
        Vehicle nonMatching = save(OTHER, "Toyota", "Casablanca",
                "4600-A-5", VehicleStatus.PUBLISHED);
        nonMatching.setCategory(categoryRepository.findByNameIgnoreCase("BERLINE").orElseThrow());
        repository.saveAndFlush(nonMatching);

        mvc.perform(get("/api/vehicles/search")
                        .param("ville", "Casablanca")
                        .param("categorie", "SUV")
                        .param("dateDebut", "2026-11-01")
                        .param("dateFin", "2026-11-05")
                        .param("prixMin", "200")
                        .param("prixMax", "400"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(matching.getId()));
    }

    @Test
    void search_withoutCity_returnsBadRequest() throws Exception {
        mvc.perform(get("/api/vehicles/search"))
                .andExpect(status().isBadRequest());
    }

    // ---------------- UPDATE ----------------

    @Test
    @WithMockUser(roles = "AGENCE")
    void put_asOwner_returns200() throws Exception {
        Vehicle v = save(
                ME, "Toyota", "Casablanca",
                "5555-A-1", VehicleStatus.PUBLISHED);

        mvc.perform(put("/api/vehicles/" + v.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body("5555-A-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modele").value("Corolla"));
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    void put_unknownVehicle_returns404() throws Exception {
        mvc.perform(put("/api/vehicles/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body("6666-A-1"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    void put_invalidBody_returns400() throws Exception {
        Map<String, Object> invalid =
                new HashMap<>(body("6666-A-2"));

        invalid.put("annee", 1980);
        invalid.put("prixJour", 0);

        Vehicle v = save(
                ME, "Toyota", "Casablanca",
                "6666-A-2", VehicleStatus.PUBLISHED);

        mvc.perform(put("/api/vehicles/" + v.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    void putAndDelete_onOtherAgencyVehicle_return403()
            throws Exception {

        Vehicle v = save(
                OTHER, "Toyota", "Casablanca",
                "6666-A-1", VehicleStatus.PUBLISHED);

        mvc.perform(put("/api/vehicles/" + v.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body("6666-A-1"))))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/vehicles/" + v.getId()))
                .andExpect(status().isForbidden());
    }

    // ---------------- DELETE ----------------

    @Test
    @WithMockUser(roles = "AGENCE")
    void delete_asOwner_softDeletes() throws Exception {
        Vehicle v = save(
                ME, "Toyota", "Casablanca",
                "7777-A-1", VehicleStatus.PUBLISHED);

        mvc.perform(delete("/api/vehicles/" + v.getId()))
                .andExpect(status().isNoContent());

        assertThat(repository.findById(v.getId())
                .orElseThrow()
                .getStatut())
                .isEqualTo(VehicleStatus.ARCHIVED);

        mvc.perform(get("/api/vehicles/" + v.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    void delete_unknownVehicle_returns404() throws Exception {
        mvc.perform(delete("/api/vehicles/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_onAnyVehicle_returns204() throws Exception {
        when(currentUser.isAdmin()).thenReturn(true);

        Vehicle v = save(
                OTHER, "Toyota", "Casablanca",
                "8888-A-1", VehicleStatus.PUBLISHED);

        mvc.perform(delete("/api/vehicles/" + v.getId()))
                .andExpect(status().isNoContent());
    }
}
