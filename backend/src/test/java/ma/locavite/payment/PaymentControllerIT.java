package ma.locavite.payment;

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
import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentMethod;
import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.dto.PaymentRequest;
import ma.locavite.payment.repository.PaymentRepository;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
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
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.cache.type=none")
@AutoConfigureMockMvc
class PaymentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

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

    private User client1;
    private User client2;
    private User agenceUser;
    private User adminUser;

    private String client1Token;
    private String client2Token;
    private String agenceToken;
    private String adminToken;

    private Vehicle vehicle;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();

        Role roleClient = roleRepository.findByName(RoleType.CLIENT.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.CLIENT.name()).build()));
        Role roleAgence = roleRepository.findByName(RoleType.AGENCE.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.AGENCE.name()).build()));
        Role roleAdmin = roleRepository.findByName(RoleType.ADMIN.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ADMIN.name()).build()));

        client1 = createUser("client1.payment@test.com", "Client", "One", roleClient);
        client2 = createUser("client2.payment@test.com", "Client", "Two", roleClient);
        agenceUser = createUser("agence.payment@test.com", "Agence", "One", roleAgence);
        adminUser = createUser("admin.payment@test.com", "Admin", "Master", roleAdmin);

        client1Token = jwtService.generateToken(client1);
        client2Token = jwtService.generateToken(client2);
        agenceToken = jwtService.generateToken(agenceUser);
        adminToken = jwtService.generateToken(adminUser);

        Brand brand = brandRepository.findByNameIgnoreCase("Dacia")
                .orElseGet(() -> {
                    Brand b = new Brand();
                    b.setName("Dacia");
                    return brandRepository.save(b);
                });
        Category category = categoryRepository.findByNameIgnoreCase("Citadine")
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setName("Citadine");
                    return categoryRepository.save(c);
                });

        vehicle = new Vehicle();
        vehicle.setBrand(brand);
        vehicle.setCategory(category);
        vehicle.setModele("Logan");
        vehicle.setAnnee(2022);
        vehicle.setVille("Casablanca");
        vehicle.setMatricule("12345-A-1");
        vehicle.setCarburant(FuelType.DIESEL);
        vehicle.setTransmission(TransmissionType.AUTOMATIQUE);
        vehicle.setPlaces(5);
        vehicle.setPrixJour(new BigDecimal("300.00"));
        vehicle.setPrixVente(new BigDecimal("120000.00"));
        vehicle.setStatut(VehicleStatus.PUBLISHED);
        vehicle.setDescription("Véhicule de test");
        vehicle.setAgenceId(agenceUser.getId());
        vehicle = vehicleRepository.save(vehicle);

        reservation = new Reservation();
        reservation.setCode("LV-PAYTEST1");
        reservation.setUserId(client1.getId());
        reservation.setVehicleId(vehicle.getId());
        reservation.setDateDebut(LocalDate.now().plusDays(3));
        reservation.setDateFin(LocalDate.now().plusDays(6));
        reservation.setLieuPriseEnCharge("Casablanca");
        reservation.setLieuRetour("Casablanca");
        reservation.setPrixTotal(new BigDecimal("1500.00"));
        reservation.setCaution(new BigDecimal("2000.00"));
        reservation.setStatut(ReservationStatus.PENDING);
        reservation = reservationRepository.save(reservation);
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

    @Test
    @DisplayName("Test : création paiement valide → statut PENDING (201 Created)")
    void createPayment_valid_returns201WithPendingStatus() throws Exception {
        PaymentRequest request = new PaymentRequest(
                reservation.getId(),
                new BigDecimal("500.00"),
                "MAD",
                PaymentMethod.CMI,
                "CMI-EXT-123",
                "{\"sessionId\":\"sess_001\"}"
        );

        mockMvc.perform(post("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/payments/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code", startsWith("PAY-")))
                .andExpect(jsonPath("$.reservationId").value(reservation.getId()))
                .andExpect(jsonPath("$.userId").value(client1.getId()))
                .andExpect(jsonPath("$.montant").value(500.0))
                .andExpect(jsonPath("$.devise").value("MAD"))
                .andExpect(jsonPath("$.methode").value("CMI"))
                .andExpect(jsonPath("$.statut").value("PENDING"))
                .andExpect(jsonPath("$.reference").value("CMI-EXT-123"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("Test : montant > prix restant → 400 Bad Request")
    void createPayment_montantExceedsRemainingPrice_returns400() throws Exception {
        // Prix total de la réservation = 1500.00, demande de paiement de 1600.00
        PaymentRequest request = new PaymentRequest(
                reservation.getId(),
                new BigDecimal("1600.00"),
                "MAD",
                PaymentMethod.CMI,
                null,
                null
        );

        mockMvc.perform(post("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("dépasse le montant restant dû")));
    }

    @Test
    @DisplayName("Test : création pour réservation inexistante → 404 Not Found")
    void createPayment_nonExistentReservation_returns404() throws Exception {
        PaymentRequest request = new PaymentRequest(
                99999L,
                new BigDecimal("100.00"),
                "MAD",
                PaymentMethod.CMI,
                null,
                null
        );

        mockMvc.perform(post("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Test : client ne voit que ses paiements (GET /api/payments/me et GET /api/payments/{id})")
    void client_canOnlySeeOwnPayments() throws Exception {
        // Création d'un paiement pour client1
        Payment payment = new Payment();
        payment.setCode("PAY-CL1001");
        payment.setReservationId(reservation.getId());
        payment.setUserId(client1.getId());
        payment.setMontant(new BigDecimal("400.00"));
        payment.setDevise("MAD");
        payment.setMethode(PaymentMethod.CMI);
        payment.setStatut(PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        Long paymentId = payment.getId();

        // 1. client1 (propriétaire) consulte avec succès
        mockMvc.perform(get("/api/payments/" + paymentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.userId").value(client1.getId()));

        // 2. client2 (tiers) tente de consulter le paiement de client1 → 403 Forbidden
        mockMvc.perform(get("/api/payments/" + paymentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client2Token))
                .andExpect(status().isForbidden());

        // 3. Admin peut consulter le paiement de client1
        mockMvc.perform(get("/api/payments/" + paymentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId));

        // 4. client2 appelle /api/payments/me → ne voit aucun paiement
        mockMvc.perform(get("/api/payments/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        // 5. client1 appelle /api/payments/me → voit son paiement
        mockMvc.perform(get("/api/payments/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(paymentId));
    }

    @Test
    @DisplayName("Test : changement de statut respecte la machine à états (cancel, confirm, fail)")
    void stateMachine_statusTransitionsEnforced() throws Exception {
        // 1. Paiement PENDING
        Payment payment = new Payment();
        payment.setCode("PAY-STAT01");
        payment.setReservationId(reservation.getId());
        payment.setUserId(client1.getId());
        payment.setMontant(new BigDecimal("300.00"));
        payment.setDevise("MAD");
        payment.setMethode(PaymentMethod.CMI);
        payment.setStatut(PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        // 2. Annulation valide PENDING → CANCELLED
        mockMvc.perform(post("/api/payments/" + payment.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CANCELLED"));

        // 3. Tentative de ré-annuler un paiement CANCELLED → 400 Bad Request
        mockMvc.perform(post("/api/payments/" + payment.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Impossible d'annuler un paiement avec le statut : CANCELLED")));

        // 4. Tentative de confirmer un paiement CANCELLED → 400 Bad Request
        mockMvc.perform(post("/api/payments/" + payment.getId() + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Impossible de confirmer un paiement avec le statut : CANCELLED")));

        // 5. Nouveau paiement PENDING confirmé à SUCCESS
        Payment payment2 = new Payment();
        payment2.setCode("PAY-STAT02");
        payment2.setReservationId(reservation.getId());
        payment2.setUserId(client1.getId());
        payment2.setMontant(new BigDecimal("400.00"));
        payment2.setDevise("MAD");
        payment2.setMethode(PaymentMethod.CMI);
        payment2.setStatut(PaymentStatus.PENDING);
        payment2 = paymentRepository.save(payment2);

        mockMvc.perform(post("/api/payments/" + payment2.getId() + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("transactionId", "TXN-999")
                        .param("reference", "REF-888"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value("TXN-999"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());

        // 6. Tentative d'annuler un paiement SUCCESS → 400 Bad Request
        mockMvc.perform(post("/api/payments/" + payment2.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Impossible d'annuler un paiement avec le statut : SUCCESS")));
    }

    @Test
    @DisplayName("Test : règle somme des paiements SUCCESS ≤ prix total réservation (acompte + solde)")
    void multiplePayments_acompteAndSolde_respectsTotalReservationPrice() throws Exception {
        // Réservation = 1500.00 MAD
        // Paiement 1 : Acompte 500.00 MAD
        PaymentRequest acompteRequest = new PaymentRequest(
                reservation.getId(),
                new BigDecimal("500.00"),
                "MAD",
                PaymentMethod.CMI,
                null,
                null
        );

        String acompteResponse = mockMvc.perform(post("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acompteRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long acompteId = objectMapper.readTree(acompteResponse).get("id").asLong();

        // Confirmation de l'acompte à SUCCESS
        mockMvc.perform(post("/api/payments/" + acompteId + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("SUCCESS"));

        // Paiement 2 : Solde 1000.00 MAD (500 + 1000 = 1500 <= 1500)
        PaymentRequest soldeRequest = new PaymentRequest(
                reservation.getId(),
                new BigDecimal("1000.00"),
                "MAD",
                PaymentMethod.CMI,
                null,
                null
        );

        String soldeResponse = mockMvc.perform(post("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(soldeRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long soldeId = objectMapper.readTree(soldeResponse).get("id").asLong();

        // Confirmation du solde à SUCCESS
        mockMvc.perform(post("/api/payments/" + soldeId + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("SUCCESS"));

        // Paiement 3 : Tentative de payer 10.00 MAD supplémentaires alors que déjà 1500/1500 payés
        PaymentRequest extraRequest = new PaymentRequest(
                reservation.getId(),
                new BigDecimal("10.00"),
                "MAD",
                PaymentMethod.CMI,
                null,
                null
        );

        mockMvc.perform(post("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extraRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("dépasse le montant restant dû (0.00)")));
    }

    @Test
    @DisplayName("Test : GET /api/payments (AGENCE, ADMIN) et interdiction pour CLIENT")
    void getAllPayments_roleBasedAccessControl() throws Exception {
        Payment payment = new Payment();
        payment.setCode("PAY-ALL001");
        payment.setReservationId(reservation.getId());
        payment.setUserId(client1.getId());
        payment.setMontant(new BigDecimal("300.00"));
        payment.setDevise("MAD");
        payment.setMethode(PaymentMethod.CMI);
        payment.setStatut(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // 1. ADMIN peut lister tous les paiements
        mockMvc.perform(get("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        // 2. AGENCE peut lister les paiements de ses véhicules
        mockMvc.perform(get("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agenceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        // 3. CLIENT ne peut pas accéder à /api/payments (réservé AGENCE/ADMIN) → 403 Forbidden
        mockMvc.perform(get("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client1Token))
                .andExpect(status().isForbidden());
    }
}
