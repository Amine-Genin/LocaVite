package ma.locavite.sale.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.catalog.repository.BrandRepository;
import ma.locavite.catalog.repository.CategoryRepository;
import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.SaleStatus;
import ma.locavite.sale.domain.VehicleCondition;
import ma.locavite.sale.dto.SaleListingRequest;
import ma.locavite.sale.repository.SaleListingRepository;
import ma.locavite.vehicle.domain.FuelType;
import ma.locavite.vehicle.domain.TransmissionType;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.domain.VehicleStatus;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.cache.type=none")
@AutoConfigureMockMvc
class SaleListingControllerIT {

    private static final Long ME = 1L;
    private static final Long OTHER = 99L;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private SaleListingRepository repository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockBean
    private CurrentUserService currentUser;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        vehicleRepository.deleteAll();

        when(currentUser.getCurrentUserId()).thenReturn(Optional.of(ME));
        when(currentUser.isAdmin()).thenReturn(false);
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
        vehicleRepository.deleteAll();
    }

    // ---------------- HELPERS ----------------

    private Vehicle createVehicle(Long agenceId, String matricule) {
        Brand brand = brandRepository.findByNameIgnoreCase("Toyota").orElseThrow();
        Category category = categoryRepository.findByNameIgnoreCase("SUV").orElseThrow();

        Vehicle v = new Vehicle();
        v.setBrand(brand);
        v.setModele("RAV4");
        v.setAnnee(2023);
        v.setCategory(category);
        v.setVille("Casablanca");
        v.setPrixJour(new BigDecimal("450"));
        v.setPrixVente(new BigDecimal("320000"));
        v.setCarburant(FuelType.DIESEL);
        v.setTransmission(TransmissionType.AUTOMATIQUE);
        v.setPlaces(5);
        v.setDescription("Toyota RAV4 Hybride/Diesel");
        v.setMatricule(matricule);
        v.setStatut(VehicleStatus.PUBLISHED);
        v.setAgenceId(agenceId);

        return vehicleRepository.saveAndFlush(v);
    }

    private SaleListingRequest createRequest(Long vehicleId, String vin, BigDecimal prixVente) {
        return new SaleListingRequest(
                vehicleId,
                "Toyota RAV4 Hybride 2023",
                "Véhicule première main en excellent état sous garantie",
                prixVente != null ? prixVente : new BigDecimal("310000.00"),
                true,
                VehicleCondition.USED,
                28000,
                true,
                LocalDate.of(2023, 3, 20),
                vin,
                1,
                SaleStatus.DRAFT
        );
    }

    private SaleListing createAndSaveListing(Vehicle vehicle, Long agenceId, SaleStatus statut, String vin) {
        SaleListing listing = new SaleListing();
        listing.setCode("SL-" + System.nanoTime() % 1000000);
        listing.setVehicle(vehicle);
        listing.setAgenceId(agenceId);
        listing.setTitre("Toyota RAV4 " + statut);
        listing.setDescription("Description de vente");
        listing.setPrixVente(new BigDecimal("300000.00"));
        listing.setPrixNego(false);
        listing.setEtat(VehicleCondition.USED);
        listing.setKilometrage(30000);
        listing.setPremierProprietaire(true);
        listing.setNumeroChassis(vin);
        listing.setStatut(statut);
        listing.setDeleted(false);
        return repository.saveAndFlush(listing);
    }

    // ---------------- CREATE ----------------

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Création d'une annonce valide par une agence -> 201 Created + Header Location")
    void post_asAgence_returns201WithLocation() throws Exception {
        Vehicle v = createVehicle(ME, "12345-A-1");
        SaleListingRequest req = createRequest(v.getId(), "WVWZZZRAV400001", new BigDecimal("310000.00"));

        mvc.perform(post("/api/sale-listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("http://localhost/api/sale-listings/")))
                .andExpect(jsonPath("$.code", startsWith("SL-")))
                .andExpect(jsonPath("$.statut").value("DRAFT"))
                .andExpect(jsonPath("$.agenceId").value(ME))
                .andExpect(jsonPath("$.vehicleId").value(v.getId()))
                .andExpect(jsonPath("$.prixVente").value(310000.00))
                .andExpect(jsonPath("$.garantieConstructeur").value(false))
                .andExpect(jsonPath("$.regimeTva").value("MARGIN"))
                .andExpect(jsonPath("$.numeroChassis").value("WVWZZZRAV400001"));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    @DisplayName("Tentative de création par un CLIENT -> 403 Forbidden")
    void post_asClient_returns403() throws Exception {
        Vehicle v = createVehicle(ME, "12345-A-2");
        SaleListingRequest req = createRequest(v.getId(), "WVWZZZRAV400002", new BigDecimal("310000.00"));

        mvc.perform(post("/api/sale-listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        assertThat(repository.count()).isEqualTo(0);
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Création pour un véhicule d'une autre agence -> 403 Forbidden")
    void post_onOtherAgencyVehicle_returns403() throws Exception {
        Vehicle v = createVehicle(OTHER, "12345-A-3");
        SaleListingRequest req = createRequest(v.getId(), "WVWZZZRAV400003", new BigDecimal("310000.00"));

        mvc.perform(post("/api/sale-listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        assertThat(repository.count()).isEqualTo(0);
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Création avec VIN déjà existant -> 409 Conflict")
    void post_duplicateVin_returns409() throws Exception {
        Vehicle v1 = createVehicle(ME, "12345-A-4");
        createAndSaveListing(v1, ME, SaleStatus.PUBLISHED, "VIN-DUPLICATE-001");

        Vehicle v2 = createVehicle(ME, "12345-A-5");
        SaleListingRequest req = createRequest(v2.getId(), "vin-duplicate-001", new BigDecimal("310000.00"));

        mvc.perform(post("/api/sale-listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Création avec prixVente <= 0 -> 400 Bad Request")
    void post_invalidPrice_returns400() throws Exception {
        Vehicle v = createVehicle(ME, "12345-A-6");

        Map<String, Object> invalid = new HashMap<>();
        invalid.put("vehicleId", v.getId());
        invalid.put("titre", "Titre valide");
        invalid.put("description", "Description détaillée valide");
        invalid.put("prixVente", 0);
        invalid.put("etat", "USED");

        mvc.perform(post("/api/sale-listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.prixVente").exists());
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Création d'une neuve avec kilométrage supérieur à 0 -> 400 Bad Request")
    void post_newVehicleWithMileage_returns400() throws Exception {
        Vehicle v = createVehicle(ME, "12345-A-7");
        Map<String, Object> invalid = new HashMap<>();
        invalid.put("vehicleId", v.getId());
        invalid.put("titre", "Toyota neuve");
        invalid.put("description", "Description détaillée");
        invalid.put("prixVente", 320000);
        invalid.put("etat", "NEW");
        invalid.put("kilometrage", 1);
        invalid.put("nombreProprietaires", 0);

        mvc.perform(post("/api/sale-listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.kilometrage").exists());

        assertThat(repository.count()).isZero();
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Création d'une occasion sans date de mise en circulation -> 400 Bad Request")
    void post_usedVehicleWithoutRegistrationDate_returns400() throws Exception {
        Vehicle v = createVehicle(ME, "12345-A-8");
        Map<String, Object> invalid = new HashMap<>();
        invalid.put("vehicleId", v.getId());
        invalid.put("titre", "Toyota occasion");
        invalid.put("description", "Description détaillée");
        invalid.put("prixVente", 280000);
        invalid.put("etat", "USED");
        invalid.put("kilometrage", 28000);
        invalid.put("nombreProprietaires", 1);

        mvc.perform(post("/api/sale-listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dateMiseEnCirculation").exists());

        assertThat(repository.count()).isZero();
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Création d'une occasion sans propriétaire déclaré -> 400 Bad Request")
    void post_usedVehicleWithoutOwner_returns400() throws Exception {
        Vehicle v = createVehicle(ME, "12345-A-10");
        Map<String, Object> invalid = new HashMap<>();
        invalid.put("vehicleId", v.getId());
        invalid.put("titre", "Toyota occasion");
        invalid.put("description", "Description détaillée");
        invalid.put("prixVente", 280000);
        invalid.put("etat", "USED");
        invalid.put("kilometrage", 28000);
        invalid.put("dateMiseEnCirculation", "2023-03-20");
        invalid.put("nombreProprietaires", 0);

        mvc.perform(post("/api/sale-listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombreProprietairesCoherent").exists());

        assertThat(repository.count()).isZero();
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Création d'une neuve valide expose la garantie et la TVA standard")
    void post_newVehicle_returnsWarrantyAndStandardVat() throws Exception {
        Vehicle v = createVehicle(ME, "12345-A-9");
        Map<String, Object> valid = new HashMap<>();
        valid.put("vehicleId", v.getId());
        valid.put("titre", "Toyota neuve");
        valid.put("description", "Description détaillée");
        valid.put("prixVente", 320000);
        valid.put("etat", "NEW");
        valid.put("kilometrage", 0);
        valid.put("nombreProprietaires", 0);

        mvc.perform(post("/api/sale-listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(valid)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.etat").value("NEW"))
                .andExpect(jsonPath("$.garantieConstructeur").value(true))
                .andExpect(jsonPath("$.regimeTva").value("STANDARD_20_PERCENT"));
    }

    // ---------------- UPDATE ----------------

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Modification par le propriétaire -> 200 OK")
    void put_asOwner_returns200() throws Exception {
        Vehicle v = createVehicle(ME, "22222-A-1");
        SaleListing listing = createAndSaveListing(v, ME, SaleStatus.DRAFT, "VIN-UPDATE-001");

        SaleListingRequest updateReq = new SaleListingRequest(
                v.getId(),
                "Nouveau Titre Modifié",
                "Nouvelle description",
                new BigDecimal("295000.00"),
                true,
                VehicleCondition.USED,
                32000,
                true,
                LocalDate.of(2023, 3, 20),
                "VIN-UPDATE-001",
                1,
                SaleStatus.DRAFT
        );

        mvc.perform(put("/api/sale-listings/" + listing.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Nouveau Titre Modifié"))
                .andExpect(jsonPath("$.prixVente").value(295000.00))
                .andExpect(jsonPath("$.kilometrage").value(32000));
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Modification par une autre agence -> 403 Forbidden")
    void put_asOtherAgency_returns403() throws Exception {
        Vehicle v = createVehicle(OTHER, "22222-A-2");
        SaleListing listing = createAndSaveListing(v, OTHER, SaleStatus.DRAFT, "VIN-UPDATE-002");

        SaleListingRequest updateReq = createRequest(v.getId(), "VIN-UPDATE-002", new BigDecimal("295000.00"));

        mvc.perform(put("/api/sale-listings/" + listing.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    // ---------------- DELETE (SOFT DELETE) ----------------

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Suppression douce par le propriétaire -> 204 No Content, statut ARCHIVED")
    void delete_asOwner_softDeletes() throws Exception {
        Vehicle v = createVehicle(ME, "33333-A-1");
        SaleListing listing = createAndSaveListing(v, ME, SaleStatus.PUBLISHED, "VIN-DELETE-001");

        mvc.perform(delete("/api/sale-listings/" + listing.getId()))
                .andExpect(status().isNoContent());

        SaleListing deleted = repository.findById(listing.getId()).orElseThrow();
        assertThat(deleted.getDeleted()).isTrue();
        assertThat(deleted.getStatut()).isEqualTo(SaleStatus.ARCHIVED);

        // GET subsequent retourne 404
        mvc.perform(get("/api/sale-listings/" + listing.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Suppression par une autre agence -> 403 Forbidden")
    void delete_asOtherAgency_returns403() throws Exception {
        Vehicle v = createVehicle(OTHER, "33333-A-2");
        SaleListing listing = createAndSaveListing(v, OTHER, SaleStatus.PUBLISHED, "VIN-DELETE-002");

        mvc.perform(delete("/api/sale-listings/" + listing.getId()))
                .andExpect(status().isForbidden());

        SaleListing unchanged = repository.findById(listing.getId()).orElseThrow();
        assertThat(unchanged.getDeleted()).isFalse();
    }

    // ---------------- PUBLISH & MARK-SOLD ----------------

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Publication par l'agence propriétaire -> 200 OK, statut PUBLISHED et publishedAt")
    void publish_asOwner_returns200() throws Exception {
        Vehicle v = createVehicle(ME, "44444-A-1");
        SaleListing listing = createAndSaveListing(v, ME, SaleStatus.DRAFT, "VIN-PUB-001");

        mvc.perform(put("/api/sale-listings/" + listing.getId() + "/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        SaleListing published = repository.findById(listing.getId()).orElseThrow();
        assertThat(published.getStatut()).isEqualTo(SaleStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isNotNull();
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Tentative de publication par une autre agence -> 403 Forbidden")
    void publish_asOtherAgency_returns403() throws Exception {
        Vehicle v = createVehicle(OTHER, "44444-A-2");
        SaleListing listing = createAndSaveListing(v, OTHER, SaleStatus.DRAFT, "VIN-PUB-002");

        mvc.perform(put("/api/sale-listings/" + listing.getId() + "/publish"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Marquage vendu par l'agence propriétaire -> 200 OK, statut SOLD et soldAt")
    void markSold_asOwner_returns200() throws Exception {
        Vehicle v = createVehicle(ME, "55555-A-1");
        SaleListing listing = createAndSaveListing(v, ME, SaleStatus.PUBLISHED, "VIN-SOLD-001");

        mvc.perform(put("/api/sale-listings/" + listing.getId() + "/mark-sold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("SOLD"))
                .andExpect(jsonPath("$.soldAt").isNotEmpty());

        SaleListing sold = repository.findById(listing.getId()).orElseThrow();
        assertThat(sold.getStatut()).isEqualTo(SaleStatus.SOLD);
        assertThat(sold.getSoldAt()).isNotNull();
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Marquage vendu par une autre agence -> 403 Forbidden")
    void markSold_asOtherAgency_returns403() throws Exception {
        Vehicle v = createVehicle(OTHER, "55555-A-2");
        SaleListing listing = createAndSaveListing(v, OTHER, SaleStatus.PUBLISHED, "VIN-SOLD-002");

        mvc.perform(put("/api/sale-listings/" + listing.getId() + "/mark-sold"))
                .andExpect(status().isForbidden());
    }

    // ---------------- PUBLIC ACCESS & SEARCH ----------------

    @Test
    @DisplayName("Consultation publique par ID pour une annonce publiée -> 200 OK")
    void getById_public_published_returns200() throws Exception {
        Vehicle v = createVehicle(OTHER, "66666-A-1");
        SaleListing listing = createAndSaveListing(v, OTHER, SaleStatus.PUBLISHED, "VIN-PUB-GET");

        mvc.perform(get("/api/sale-listings/" + listing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listing.getId()))
                .andExpect(jsonPath("$.statut").value("PUBLISHED"));
    }

    @Test
    @DisplayName("Consultation publique par ID pour un brouillon -> 404 Not Found")
    void getById_public_draft_returns404() throws Exception {
        Vehicle v = createVehicle(OTHER, "66666-A-2");
        SaleListing listing = createAndSaveListing(v, OTHER, SaleStatus.DRAFT, "VIN-DRAFT-GET");

        mvc.perform(get("/api/sale-listings/" + listing.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Recherche publique : filtre uniquement les annonces publiées et paginées")
    void search_public_returnsOnlyPublished() throws Exception {
        Vehicle v = createVehicle(OTHER, "77777-A-1");
        createAndSaveListing(v, OTHER, SaleStatus.PUBLISHED, "VIN-SEARCH-1");
        createAndSaveListing(v, OTHER, SaleStatus.DRAFT, "VIN-SEARCH-2");

        mvc.perform(get("/api/sale-listings")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].statut").value("PUBLISHED"));
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    @DisplayName("Un véhicule peut avoir plusieurs annonces (historique)")
    void vehicle_canHaveMultipleSaleListings() throws Exception {
        Vehicle v = createVehicle(ME, "88888-A-1");
        createAndSaveListing(v, ME, SaleStatus.SOLD, "VIN-HIST-1");
        createAndSaveListing(v, ME, SaleStatus.PUBLISHED, "VIN-HIST-2");

        assertThat(repository.findByVehicleIdAndDeletedFalse(v.getId())).hasSize(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Un ADMIN peut supprimer n'importe quelle annonce de vente -> 204")
    void delete_asAdmin_returns204() throws Exception {
        when(currentUser.isAdmin()).thenReturn(true);

        Vehicle v = createVehicle(OTHER, "99999-A-1");
        SaleListing listing = createAndSaveListing(v, OTHER, SaleStatus.PUBLISHED, "VIN-ADMIN-DEL");

        mvc.perform(delete("/api/sale-listings/" + listing.getId()))
                .andExpect(status().isNoContent());

        SaleListing deleted = repository.findById(listing.getId()).orElseThrow();
        assertThat(deleted.getDeleted()).isTrue();
    }
}
