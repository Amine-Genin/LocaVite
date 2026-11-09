package ma.locavite.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.RoleType;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.RoleRepository;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.auth.service.JwtService;
import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.catalog.repository.BrandRepository;
import ma.locavite.catalog.repository.CategoryRepository;
import ma.locavite.document.domain.Document;
import ma.locavite.document.domain.DocumentStatus;
import ma.locavite.document.domain.DocumentType;
import ma.locavite.document.repository.DocumentRepository;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationOption;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.dto.ReservationRequest;
import ma.locavite.reservation.dto.QuoteRequest;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.FuelType;
import ma.locavite.vehicle.domain.TransmissionType;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.domain.VehicleStatus;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.cache.type=none")
@AutoConfigureMockMvc
class ReservationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User clientWithDocs;
    private User clientWithoutDocs;
    private User agence1;
    private User agence2;
    private User adminUser;

    private String clientWithDocsToken;
    private String clientWithoutDocsToken;
    private String agence1Token;
    private String agence2Token;
    private String adminToken;

    private Vehicle vehicleAgence1;
    private Vehicle vehicleAgence2;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        documentRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();

        Role roleClient = roleRepository.findByName(RoleType.CLIENT.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.CLIENT.name()).build()));
        Role roleAgence = roleRepository.findByName(RoleType.AGENCE.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.AGENCE.name()).build()));
        Role roleAdmin = roleRepository.findByName(RoleType.ADMIN.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ADMIN.name()).build()));

        // 1. Clients
        clientWithDocs = createUser("client.valid@test.com", "Client", "Valid", roleClient);
        clientWithoutDocs = createUser("client.invalid@test.com", "Client", "Invalid", roleClient);

        // Upload validated documents for clientWithDocs (permis + CIN)
        createDocument(clientWithDocs, DocumentType.PERMIS, DocumentStatus.VALIDATED);
        createDocument(clientWithDocs, DocumentType.CIN, DocumentStatus.VALIDATED);

        // 2. Agences
        agence1 = createUser("agence1@test.com", "Agence", "One", roleAgence);
        agence2 = createUser("agence2@test.com", "Agence", "Two", roleAgence);

        // 3. Admin
        adminUser = createUser("admin@test.com", "Admin", "Super", roleAdmin);

        // Tokens
        clientWithDocsToken = jwtService.generateToken(clientWithDocs);
        clientWithoutDocsToken = jwtService.generateToken(clientWithoutDocs);
        agence1Token = jwtService.generateToken(agence1);
        agence2Token = jwtService.generateToken(agence2);
        adminToken = jwtService.generateToken(adminUser);

        // Vehicles
        Brand brand = brandRepository.findByNameIgnoreCase("Toyota")
                .orElseGet(() -> {
                    Brand b = new Brand();
                    b.setName("Toyota");
                    return brandRepository.save(b);
                });
        Category category = categoryRepository.findByNameIgnoreCase("SUV")
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setName("SUV");
                    return categoryRepository.save(c);
                });

        vehicleAgence1 = createVehicle(brand, category, "1111-A-1", agence1.getId(), new BigDecimal("400.00"));
        vehicleAgence2 = createVehicle(brand, category, "2222-B-2", agence2.getId(), new BigDecimal("600.00"));
    }

    private User createUser(String email, String nom, String prenom, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setTelephone("+212600000000");
        u.setPassword(passwordEncoder.encode("Password123!"));
        u.setRoles(Set.of(role));
        return userRepository.save(u);
    }

    private Document createDocument(User user, DocumentType type, DocumentStatus status) {
        Document doc = new Document();
        doc.setUser(user);
        doc.setType(type);
        doc.setStatus(status);
        doc.setPath("documents/" + user.getId() + "_" + type.name() + ".pdf");
        return documentRepository.save(doc);
    }

    private Vehicle createVehicle(Brand brand, Category category, String matricule, Long agenceId, BigDecimal prixJour) {
        Vehicle v = new Vehicle();
        v.setBrand(brand);
        v.setCategory(category);
        v.setModele("RAV4");
        v.setAnnee(2022);
        v.setVille("Casablanca");
        v.setPrixJour(prixJour);
        v.setPrixVente(new BigDecimal("250000.00"));
        v.setCarburant(FuelType.HYBRIDE);
        v.setTransmission(TransmissionType.AUTOMATIQUE);
        v.setPlaces(5);
        v.setDescription("Excellent SUV");
        v.setMatricule(matricule);
        v.setStatut(VehicleStatus.PUBLISHED);
        v.setAgenceId(agenceId);
        return vehicleRepository.save(v);
    }

    @Test
    @DisplayName("Test : création valide → 201 + code généré (format LV-XXXXXX)")
    void post_validReservation_returns201WithCodeAndLocation() throws Exception {
        LocalDate debut = LocalDate.now().plusDays(2);
        LocalDate fin = LocalDate.now().plusDays(6); // 4 jours

        ReservationRequest request = new ReservationRequest(
                vehicleAgence1.getId(),
                debut,
                fin,
                "Aéroport Med V",
                "Aéroport Med V",
                Set.of(ReservationOption.GPS, ReservationOption.EXTRA_DRIVER)
        );

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientWithDocsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code", matchesPattern("^LV-[A-Z0-9]{6}$")))
                .andExpect(jsonPath("$.statut").value("PENDING"))
                .andExpect(jsonPath("$.vehicleId").value(vehicleAgence1.getId()))
                .andExpect(jsonPath("$.userId").value(clientWithDocs.getId()))
                // 4 jours * 400 + 4 jours d'options + caution = 5200.00
                .andExpect(jsonPath("$.prixTotal").value(5200.0))
                .andExpect(jsonPath("$.caution").value(3000.0));
    }

    @Test
    @DisplayName("Test : dates invalides (dateDebut >= dateFin) → 400")
    void post_invalidDates_dateDebutAfterDateFin_returns400() throws Exception {
        LocalDate debut = LocalDate.now().plusDays(6);
        LocalDate fin = LocalDate.now().plusDays(2);

        ReservationRequest request = new ReservationRequest(
                vehicleAgence1.getId(),
                debut,
                fin,
                "Aéroport Med V",
                "Aéroport Med V",
                Collections.emptySet()
        );

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientWithDocsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test : dates invalides (dateDebut dans le passé) → 400")
    void post_invalidDates_dateDebutInPast_returns400() throws Exception {
        LocalDate debut = LocalDate.now().minusDays(2);
        LocalDate fin = LocalDate.now().plusDays(2);

        ReservationRequest request = new ReservationRequest(
                vehicleAgence1.getId(),
                debut,
                fin,
                "Aéroport Med V",
                "Aéroport Med V",
                Collections.emptySet()
        );

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientWithDocsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test : véhicule déjà réservé sur la période → 409 Conflict")
    void post_whenVehicleAlreadyReserved_returns409Conflict() throws Exception {
        LocalDate debut = LocalDate.now().plusDays(2);
        LocalDate fin = LocalDate.now().plusDays(6);

        // Créer une première réservation existante
        Reservation first = new Reservation();
        first.setCode("LV-EXIST1");
        first.setUserId(clientWithDocs.getId());
        first.setVehicleId(vehicleAgence1.getId());
        first.setDateDebut(debut);
        first.setDateFin(fin);
        first.setLieuPriseEnCharge("Gare");
        first.setLieuRetour("Gare");
        first.setPrixTotal(new BigDecimal("1600.00"));
        first.setCaution(new BigDecimal("3000.00"));
        first.setStatut(ReservationStatus.CONFIRMED);
        reservationRepository.save(first);

        // Nouvelle réservation chevauchante
        ReservationRequest overlappingRequest = new ReservationRequest(
                vehicleAgence1.getId(),
                debut.plusDays(1),
                fin.plusDays(2),
                "Aéroport",
                "Aéroport",
                Collections.emptySet()
        );

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientWithDocsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlappingRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Test : client sans documents validés → 403 Forbidden")
    void post_whenClientDocumentsNotValidated_returns403() throws Exception {
        LocalDate debut = LocalDate.now().plusDays(2);
        LocalDate fin = LocalDate.now().plusDays(5);

        ReservationRequest request = new ReservationRequest(
                vehicleAgence1.getId(),
                debut,
                fin,
                "Aéroport",
                "Aéroport",
                Collections.emptySet()
        );

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientWithoutDocsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test : client ne peut voir que ses réservations (GET /api/reservations/{id})")
    void getById_clientCannotViewOtherClientReservation_returns403() throws Exception {
        Reservation res = new Reservation();
        res.setCode("LV-SECURE");
        res.setUserId(clientWithDocs.getId());
        res.setVehicleId(vehicleAgence1.getId());
        res.setDateDebut(LocalDate.now().plusDays(2));
        res.setDateFin(LocalDate.now().plusDays(5));
        res.setLieuPriseEnCharge("Casa");
        res.setLieuRetour("Casa");
        res.setPrixTotal(new BigDecimal("1200.00"));
        res.setCaution(new BigDecimal("3000.00"));
        res.setStatut(ReservationStatus.PENDING);
        res = reservationRepository.save(res);

        // Le propriétaire peut consulter (200 OK)
        mockMvc.perform(get("/api/reservations/" + res.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientWithDocsToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LV-SECURE"));

        // Un autre client reçoit 403 Forbidden
        mockMvc.perform(get("/api/reservations/" + res.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientWithoutDocsToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test : agence voit les réservations de ses véhicules")
    void get_agencyOnlySeesReservationsForItsVehicles() throws Exception {
        // Réservation sur le véhicule de l'agence 1
        Reservation res1 = new Reservation();
        res1.setCode("LV-AG1001");
        res1.setUserId(clientWithDocs.getId());
        res1.setVehicleId(vehicleAgence1.getId());
        res1.setDateDebut(LocalDate.now().plusDays(2));
        res1.setDateFin(LocalDate.now().plusDays(4));
        res1.setLieuPriseEnCharge("Casa");
        res1.setLieuRetour("Casa");
        res1.setPrixTotal(new BigDecimal("800.00"));
        res1.setCaution(new BigDecimal("3000.00"));
        res1.setStatut(ReservationStatus.CONFIRMED);
        res1 = reservationRepository.save(res1);

        // Réservation sur le véhicule de l'agence 2
        Reservation res2 = new Reservation();
        res2.setCode("LV-AG2001");
        res2.setUserId(clientWithDocs.getId());
        res2.setVehicleId(vehicleAgence2.getId());
        res2.setDateDebut(LocalDate.now().plusDays(5));
        res2.setDateFin(LocalDate.now().plusDays(8));
        res2.setLieuPriseEnCharge("Rabat");
        res2.setLieuRetour("Rabat");
        res2.setPrixTotal(new BigDecimal("1800.00"));
        res2.setCaution(new BigDecimal("3000.00"));
        res2.setStatut(ReservationStatus.CONFIRMED);
        res2 = reservationRepository.save(res2);

        // Agence 1 liste toutes les réservations : voit seulement res1
        mockMvc.perform(get("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agence1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].code").value("LV-AG1001"));

        // Agence 1 consulte res1 : 200 OK
        mockMvc.perform(get("/api/reservations/" + res1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agence1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LV-AG1001"));

        // Agence 1 consulte res2 : 403 Forbidden
        mockMvc.perform(get("/api/reservations/" + res2.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agence1Token))
                .andExpect(status().isForbidden());

        // Admin liste toutes les réservations : voit les deux
        mockMvc.perform(get("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("Test : client consulte sa liste de réservations (GET /api/reservations/me)")
    void getMe_returnsOnlyClientReservations() throws Exception {
        Reservation res = new Reservation();
        res.setCode("LV-MYRES1");
        res.setUserId(clientWithDocs.getId());
        res.setVehicleId(vehicleAgence1.getId());
        res.setDateDebut(LocalDate.now().plusDays(2));
        res.setDateFin(LocalDate.now().plusDays(4));
        res.setLieuPriseEnCharge("Casa");
        res.setLieuRetour("Casa");
        res.setPrixTotal(new BigDecimal("800.00"));
        res.setCaution(new BigDecimal("3000.00"));
        res.setStatut(ReservationStatus.PENDING);
        reservationRepository.save(res);

        mockMvc.perform(get("/api/reservations/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientWithDocsToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].code").value("LV-MYRES1"));

        mockMvc.perform(get("/api/reservations/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientWithoutDocsToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("Test : modification autorisée si PENDING (PUT /api/reservations/{id})")
    void put_whenPending_updatesReservation() throws Exception {
        Reservation res = new Reservation();
        res.setCode("LV-UPDATE1");
        res.setUserId(clientWithDocs.getId());
        res.setVehicleId(vehicleAgence1.getId());
        res.setDateDebut(LocalDate.now().plusDays(2));
        res.setDateFin(LocalDate.now().plusDays(4));
        res.setLieuPriseEnCharge("Casa");
        res.setLieuRetour("Casa");
        res.setPrixTotal(new BigDecimal("800.00"));
        res.setCaution(new BigDecimal("3000.00"));
        res.setStatut(ReservationStatus.PENDING);
        res = reservationRepository.save(res);

        ReservationRequest updateRequest = new ReservationRequest(
                vehicleAgence1.getId(),
                LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(6), // 3 jours
                "Gare Oasis",
                "Gare Oasis",
                Set.of(ReservationOption.GPS)
        );

        mockMvc.perform(put("/api/reservations/" + res.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientWithDocsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lieuPriseEnCharge").value("Gare Oasis"))
                // 3 * 400 + 3 jours de GPS + caution = 4350.00
                .andExpect(jsonPath("$.prixTotal").value(4350.0));
    }

    @Test
    @DisplayName("Test : devis public sans création de réservation")
    void quote_returnsPriceBreakdownWithoutCreatingReservation() throws Exception {
        QuoteRequest request = new QuoteRequest(
                vehicleAgence1.getId(),
                LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(5),
                Set.of(ReservationOption.GPS)
        );

        mockMvc.perform(post("/api/reservations/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sousTotal").value(1200.0))
                .andExpect(jsonPath("$.options").value(150.0))
                .andExpect(jsonPath("$.remise").value(0.0))
                .andExpect(jsonPath("$.caution").value(3000.0))
                .andExpect(jsonPath("$.tva").value(0.0))
                .andExpect(jsonPath("$.total").value(4350.0));

        org.assertj.core.api.Assertions.assertThat(reservationRepository.count()).isZero();

        ReservationRequest reservationRequest = new ReservationRequest(
                vehicleAgence1.getId(),
                request.dateDebut(),
                request.dateFin(),
                "Aéroport Med V",
                "Aéroport Med V",
                request.options()
        );
        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientWithDocsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixTotal").value(4350.0));
    }

    @Test
    @DisplayName("Test : annulation réservation (DELETE /api/reservations/{id}) → statut CANCELLED")
    void delete_cancelsReservation() throws Exception {
        Reservation res = new Reservation();
        res.setCode("LV-CANCEL1");
        res.setUserId(clientWithDocs.getId());
        res.setVehicleId(vehicleAgence1.getId());
        res.setDateDebut(LocalDate.now().plusDays(2));
        res.setDateFin(LocalDate.now().plusDays(4));
        res.setLieuPriseEnCharge("Casa");
        res.setLieuRetour("Casa");
        res.setPrixTotal(new BigDecimal("800.00"));
        res.setCaution(new BigDecimal("3000.00"));
        res.setStatut(ReservationStatus.PENDING);
        res = reservationRepository.save(res);

        mockMvc.perform(delete("/api/reservations/" + res.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientWithDocsToken))
                .andExpect(status().isNoContent());

        Reservation updated = reservationRepository.findById(res.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.CANCELLED, updated.getStatut());
    }
}
