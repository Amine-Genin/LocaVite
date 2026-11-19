package ma.locavite.sale.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.catalog.repository.BrandRepository;
import ma.locavite.catalog.repository.CategoryRepository;
import ma.locavite.sale.domain.HistoryEventType;
import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.SaleStatus;
import ma.locavite.sale.domain.VehicleCondition;
import ma.locavite.sale.domain.VehicleHistory;
import ma.locavite.sale.repository.SaleListingRepository;
import ma.locavite.sale.repository.VehicleHistoryRepository;
import ma.locavite.sale.service.VehicleHistoryDocumentStorageService;
import ma.locavite.vehicle.domain.FuelType;
import ma.locavite.vehicle.domain.TransmissionType;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.domain.VehicleStatus;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.cache.type=none")
@AutoConfigureMockMvc
class VehicleHistoryControllerIT {

    private static final Long AGENCY_ID = 1L;
    private static final Long OTHER_AGENCY_ID = 99L;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaleListingRepository saleListingRepository;

    @Autowired
    private VehicleHistoryRepository historyRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockBean
    private CurrentUserService currentUser;

    @MockBean
    private VehicleHistoryDocumentStorageService documentStorage;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        saleListingRepository.deleteAll();
        vehicleRepository.deleteAll();
        when(currentUser.getCurrentUserId()).thenReturn(Optional.of(AGENCY_ID));
        when(currentUser.isAdmin()).thenReturn(false);
    }

    @AfterEach
    void cleanUp() {
        historyRepository.deleteAll();
        saleListingRepository.deleteAll();
        vehicleRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void publishedHistoryIsVisibleAndSortedChronologically() throws Exception {
        SaleListing listing = createListing(AGENCY_ID, SaleStatus.PUBLISHED);
        saveEvent(listing, LocalDate.of(2024, 5, 1), HistoryEventType.VIDANGE);
        saveEvent(listing, LocalDate.of(2023, 5, 1), HistoryEventType.MAINTENANCE);

        mvc.perform(get("/api/sale-listings/{id}/history", listing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].typeEvenement").value("MAINTENANCE"))
                .andExpect(jsonPath("$[0].date").value("2023-05-01"))
                .andExpect(jsonPath("$[1].typeEvenement").value("VIDANGE"));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void unpublishedHistoryIsHiddenFromClients() throws Exception {
        SaleListing listing = createListing(OTHER_AGENCY_ID, SaleStatus.DRAFT);
        saveEvent(listing, LocalDate.of(2024, 5, 1), HistoryEventType.REPARATION);

        mvc.perform(get("/api/sale-listings/{id}/history", listing.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    void agencyCanAddAnEventAndUploadItsDocument() throws Exception {
        SaleListing listing = createListing(AGENCY_ID, SaleStatus.PUBLISHED);
        MockMultipartFile document = new MockMultipartFile(
                "document", "facture.pdf", MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4\n%%EOF".getBytes());
        when(documentStorage.upload(eq(listing.getId()), any())).thenReturn(
                "sale-listings/" + listing.getId() + "/history/facture.pdf");
        when(documentStorage.getPresignedUrl(any())).thenReturn("https://minio.example/facture.pdf");

        mvc.perform(multipart("/api/admin/sale-listings/{id}/history", listing.getId())
                        .file(document)
                        .part(historyRequestPart("VIDANGE", "2024-06-15", "Vidange et filtre remplacés", 42000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saleListingId").value(listing.getId()))
                .andExpect(jsonPath("$.vehicleId").value(listing.getVehicle().getId()))
                .andExpect(jsonPath("$.typeEvenement").value("VIDANGE"))
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.documentUrl").value("https://minio.example/facture.pdf"));

        verify(documentStorage).upload(eq(listing.getId()), any());
        org.assertj.core.api.Assertions.assertThat(historyRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    void agencyCannotAddEventsToAnotherAgencysListing() throws Exception {
        SaleListing listing = createListing(OTHER_AGENCY_ID, SaleStatus.PUBLISHED);

        mvc.perform(multipart("/api/admin/sale-listings/{id}/history", listing.getId())
                        .part(historyRequestPart("VIDANGE", "2024-06-15", "Vidange", 42000)))
                .andExpect(status().isForbidden());

        org.assertj.core.api.Assertions.assertThat(historyRepository.count()).isZero();
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void clientCannotUseAgencyUploadEndpoint() throws Exception {
        SaleListing listing = createListing(AGENCY_ID, SaleStatus.PUBLISHED);

        mvc.perform(multipart("/api/admin/sale-listings/{id}/history", listing.getId())
                        .part(historyRequestPart("VIDANGE", "2024-06-15", "Vidange", 42000)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "AGENCE")
    void agencyCannotAddAnEventWithNegativeMileage() throws Exception {
        SaleListing listing = createListing(AGENCY_ID, SaleStatus.PUBLISHED);

        mvc.perform(multipart("/api/admin/sale-listings/{id}/history", listing.getId())
                        .part(historyRequestPart("VIDANGE", "2024-06-15", "Vidange", -1)))
                .andExpect(status().isBadRequest());

        org.assertj.core.api.Assertions.assertThat(historyRepository.count()).isZero();
    }

    private SaleListing createListing(Long agencyId, SaleStatus status) {
        Brand brand = brandRepository.findByNameIgnoreCase("Toyota").orElseThrow();
        Category category = categoryRepository.findByNameIgnoreCase("SUV").orElseThrow();
        Vehicle vehicle = new Vehicle();
        vehicle.setBrand(brand);
        vehicle.setModele("RAV4");
        vehicle.setAnnee(2023);
        vehicle.setCategory(category);
        vehicle.setVille("Casablanca");
        vehicle.setPrixJour(new BigDecimal("450"));
        vehicle.setPrixVente(new BigDecimal("320000"));
        vehicle.setCarburant(FuelType.DIESEL);
        vehicle.setTransmission(TransmissionType.AUTOMATIQUE);
        vehicle.setPlaces(5);
        vehicle.setDescription("Toyota RAV4");
        vehicle.setMatricule("12345-A-1");
        vehicle.setStatut(VehicleStatus.PUBLISHED);
        vehicle.setAgenceId(agencyId);
        vehicle = vehicleRepository.saveAndFlush(vehicle);

        SaleListing listing = new SaleListing();
        listing.setCode("SL-" + System.nanoTime());
        listing.setVehicle(vehicle);
        listing.setAgenceId(agencyId);
        listing.setTitre("Toyota RAV4 occasion");
        listing.setDescription("Annonce de test");
        listing.setPrixVente(new BigDecimal("300000"));
        listing.setEtat(VehicleCondition.USED);
        listing.setKilometrage(42000);
        listing.setDateMiseEnCirculation(LocalDate.of(2022, 1, 1));
        listing.setNombreProprietaires(1);
        listing.setStatut(status);
        listing.setDeleted(false);
        return saleListingRepository.saveAndFlush(listing);
    }

    private void saveEvent(SaleListing listing, LocalDate date, HistoryEventType type) {
        VehicleHistory event = new VehicleHistory();
        event.setSaleListing(listing);
        event.setDate(date);
        event.setTypeEvenement(type);
        event.setDescription("Événement de test");
        event.setKilometrage(42000);
        historyRepository.saveAndFlush(event);
    }

    private MockPart historyRequestPart(String type, String date, String description, int kilometrage)
            throws Exception {
        byte[] content = objectMapper.writeValueAsBytes(java.util.Map.of(
                "typeEvenement", type,
                "date", date,
                "description", description,
                "kilometrage", kilometrage));
        MockPart part = new MockPart("request", content);
        part.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return part;
    }
}
