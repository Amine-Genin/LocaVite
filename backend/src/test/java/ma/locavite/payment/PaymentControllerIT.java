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
import ma.locavite.mail.repository.EmailLogRepository;
import ma.locavite.notification.repository.NotificationLogRepository;
import ma.locavite.payment.domain.Invoice;
import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentMethod;
import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.dto.PaymentRequest;
import ma.locavite.payment.dto.RefundRequest;
import ma.locavite.payment.repository.InvoiceRepository;
import ma.locavite.payment.repository.PaymentRepository;
import ma.locavite.payment.repository.RefundRepository;
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
    private InvoiceRepository invoiceRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private EmailLogRepository emailLogRepository;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

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
        refundRepository.deleteAll();
        invoiceRepository.deleteAll();
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

    @Test
    @DisplayName("Test : création paiement cash (CASH_DELIVERY et CASH_AGENCY) → PENDING")
    void createCashPayment_valid_returns201WithPendingStatus() throws Exception {
        String body = objectMapper.writeValueAsString(
                new ma.locavite.payment.dto.CashPaymentRequest(reservation.getId(), PaymentMethod.CASH_DELIVERY)
        );

        mockMvc.perform(post("/api/payments/cash")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code", startsWith("PAY-")))
                .andExpect(jsonPath("$.reservationId").value(reservation.getId()))
                .andExpect(jsonPath("$.userId").value(client1.getId()))
                .andExpect(jsonPath("$.montant").value(1500.0))
                .andExpect(jsonPath("$.devise").value("MAD"))
                .andExpect(jsonPath("$.methode").value("CASH_DELIVERY"))
                .andExpect(jsonPath("$.statut").value("PENDING"))
                .andExpect(jsonPath("$.transactionId").isEmpty())
                .andExpect(jsonPath("$.paidAt").isEmpty());
    }

    @Test
    @DisplayName("Test : confirmation agence → SUCCESS (PUT /api/admin/payments/{id}/confirm-cash)")
    void confirmCash_byAgence_returns200WithSuccessStatus() throws Exception {
        Payment cashPayment = new Payment();
        cashPayment.setCode("PAY-CSH100");
        cashPayment.setReservationId(reservation.getId());
        cashPayment.setUserId(client1.getId());
        cashPayment.setMontant(new BigDecimal("1500.00"));
        cashPayment.setDevise("MAD");
        cashPayment.setMethode(PaymentMethod.CASH_AGENCY);
        cashPayment.setStatut(PaymentStatus.PENDING);
        cashPayment = paymentRepository.save(cashPayment);

        mockMvc.perform(put("/api/admin/payments/" + cashPayment.getId() + "/confirm-cash")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agenceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cashPayment.getId()))
                .andExpect(jsonPath("$.methode").value("CASH_AGENCY"))
                .andExpect(jsonPath("$.statut").value("SUCCESS"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty())
                .andExpect(jsonPath("$.reference", startsWith("TICKET-")));
    }

    @Test
    @DisplayName("Test : reçu PDF généré (ticket de caisse)")
    void confirmCash_generatesValidPdfReceipt() throws Exception {
        Payment cashPayment = new Payment();
        cashPayment.setCode("PAY-CSH200");
        cashPayment.setReservationId(reservation.getId());
        cashPayment.setUserId(client1.getId());
        cashPayment.setMontant(new BigDecimal("1500.00"));
        cashPayment.setDevise("MAD");
        cashPayment.setMethode(PaymentMethod.CASH_DELIVERY);
        cashPayment.setStatut(PaymentStatus.PENDING);
        cashPayment = paymentRepository.save(cashPayment);

        // Confirmation par l'agence
        mockMvc.perform(put("/api/admin/payments/" + cashPayment.getId() + "/confirm-cash")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agenceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("SUCCESS"));

        // Téléchargement du reçu PDF (ticket de caisse)
        byte[] pdfBytes = mockMvc.perform(get("/api/admin/payments/" + cashPayment.getId() + "/receipt")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agenceToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        try (var pdf = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            String text = new org.apache.pdfbox.text.PDFTextStripper().getText(pdf).replaceAll("\\s+", " ");
            org.assertj.core.api.Assertions.assertThat(text)
                    .contains("Ticket de caisse", "PAY-CSH200", "LV-PAYTEST1", "CASH_DELIVERY", "1500.00");
        }
    }

    @Test
    @DisplayName("Test : client ne peut pas confirmer lui-même (403 Forbidden)")
    void confirmCash_byClient_returns403Forbidden() throws Exception {
        Payment cashPayment = new Payment();
        cashPayment.setCode("PAY-CSH300");
        cashPayment.setReservationId(reservation.getId());
        cashPayment.setUserId(client1.getId());
        cashPayment.setMontant(new BigDecimal("1500.00"));
        cashPayment.setDevise("MAD");
        cashPayment.setMethode(PaymentMethod.CASH_DELIVERY);
        cashPayment.setStatut(PaymentStatus.PENDING);
        cashPayment = paymentRepository.save(cashPayment);

        mockMvc.perform(put("/api/admin/payments/" + cashPayment.getId() + "/confirm-cash")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test : facture générée après paiement SUCCESS + numéro séquentiel sans trou")
    void confirmPayments_generatesInvoicesAutomaticallyWithSequentialNumbersWithoutGaps() throws Exception {
        Payment p1 = new Payment();
        p1.setCode("PAY-INV001");
        p1.setReservationId(reservation.getId());
        p1.setUserId(client1.getId());
        p1.setMontant(new BigDecimal("600.00"));
        p1.setDevise("MAD");
        p1.setMethode(PaymentMethod.CMI);
        p1.setStatut(PaymentStatus.PENDING);
        p1 = paymentRepository.save(p1);

        Payment p2 = new Payment();
        p2.setCode("PAY-INV002");
        p2.setReservationId(reservation.getId());
        p2.setUserId(client1.getId());
        p2.setMontant(new BigDecimal("900.00"));
        p2.setDevise("MAD");
        p2.setMethode(PaymentMethod.CASH_AGENCY);
        p2.setStatut(PaymentStatus.PENDING);
        p2 = paymentRepository.save(p2);

        // Confirmation du 1er paiement (CMI)
        mockMvc.perform(post("/api/payments/" + p1.getId() + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("SUCCESS"));

        // Confirmation du 2e paiement (Espèces en agence)
        mockMvc.perform(put("/api/admin/payments/" + p2.getId() + "/confirm-cash")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agenceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("SUCCESS"));

        Invoice inv1 = invoiceRepository.findByPaymentId(p1.getId()).orElseThrow();
        Invoice inv2 = invoiceRepository.findByPaymentId(p2.getId()).orElseThrow();

        org.assertj.core.api.Assertions.assertThat(inv1.getNumero()).matches("FA-\\d{4}-\\d{4}");
        org.assertj.core.api.Assertions.assertThat(inv2.getNumero()).matches("FA-\\d{4}-\\d{4}");

        int seq1 = Integer.parseInt(inv1.getNumero().substring(inv1.getNumero().lastIndexOf('-') + 1));
        int seq2 = Integer.parseInt(inv2.getNumero().substring(inv2.getNumero().lastIndexOf('-') + 1));
        org.assertj.core.api.Assertions.assertThat(seq2).isEqualTo(seq1 + 1);

        org.assertj.core.api.Assertions.assertThat(inv1.getMontantTTC()).isEqualByComparingTo("600.00");
        org.assertj.core.api.Assertions.assertThat(inv1.getMontantHT()).isEqualByComparingTo("500.00");
        org.assertj.core.api.Assertions.assertThat(inv1.getMontantTVA()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Test : téléchargement facture PDF par propriétaire OK (GET /api/payments/{id}/invoice) + contenu PDFBox")
    void getInvoice_byOwner_returns200AndValidPdfContent() throws Exception {
        Payment payment = new Payment();
        payment.setCode("PAY-INV200");
        payment.setReservationId(reservation.getId());
        payment.setUserId(client1.getId());
        payment.setMontant(new BigDecimal("1200.00"));
        payment.setDevise("MAD");
        payment.setMethode(PaymentMethod.CMI);
        payment.setStatut(PaymentStatus.PENDING);
        payment.setMetadata("{\"clientIce\":\"009988776655443\",\"clientAddress\":\"10 Rue de la Liberté, Casablanca\"}");
        payment = paymentRepository.save(payment);

        mockMvc.perform(post("/api/payments/" + payment.getId() + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        Invoice invoice = invoiceRepository.findByPaymentId(payment.getId()).orElseThrow();

        byte[] pdfBytes = mockMvc.perform(get("/api/payments/" + payment.getId() + "/invoice")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(invoice.getNumero())))
                .andReturn().getResponse().getContentAsByteArray();

        try (var pdf = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            String text = new org.apache.pdfbox.text.PDFTextStripper().getText(pdf).replaceAll("\\s+", " ");
            org.assertj.core.api.Assertions.assertThat(text).contains(
                    "LocaVite SARL",
                    "002584963000084",
                    "458921",
                    "50412896",
                    "34298115",
                    invoice.getNumero(),
                    "One Client",
                    "009988776655443",
                    "10 Rue de la Liberté, Casablanca",
                    "1000.00 MAD",
                    "200.00 MAD",
                    "1200.00 MAD",
                    "TVA 20%",
                    "CMI",
                    "QR code de vérification"
            );
        }
    }

    @Test
    @DisplayName("Test : téléchargement facture par un autre client → 403 Forbidden")
    void getInvoice_byOtherClient_returns403Forbidden() throws Exception {
        Payment payment = new Payment();
        payment.setCode("PAY-INV403");
        payment.setReservationId(reservation.getId());
        payment.setUserId(client1.getId());
        payment.setMontant(new BigDecimal("1200.00"));
        payment.setDevise("MAD");
        payment.setMethode(PaymentMethod.CMI);
        payment.setStatut(PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        mockMvc.perform(post("/api/payments/" + payment.getId() + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/payments/" + payment.getId() + "/invoice")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test : remboursement total → statut REFUNDED (POST /api/admin/payments/{id}/refund)")
    void refundPayment_fullAmount_transitionsToRefunded() throws Exception {
        Payment payment = new Payment();
        payment.setCode("PAY-REF100");
        payment.setReservationId(reservation.getId());
        payment.setUserId(client1.getId());
        payment.setMontant(new BigDecimal("1500.00"));
        payment.setDevise("MAD");
        payment.setMethode(PaymentMethod.CMI);
        payment.setStatut(PaymentStatus.SUCCESS);
        payment.setTransactionId("CMI-TX-100");
        payment = paymentRepository.save(payment);

        RefundRequest refundRequest = new RefundRequest(
                new BigDecimal("1500.00"),
                "Annulation à plus de 7 jours avant départ (100%)"
        );

        mockMvc.perform(post("/api/admin/payments/" + payment.getId() + "/refund")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agenceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.paymentId").value(payment.getId()))
                .andExpect(jsonPath("$.paymentStatut").value("REFUNDED"))
                .andExpect(jsonPath("$.montant").value(1500.0))
                .andExpect(jsonPath("$.montantTotalRembourse").value(1500.0))
                .andExpect(jsonPath("$.montantRestant").value(0.0))
                .andExpect(jsonPath("$.statut").value("COMPLETED"))
                .andExpect(jsonPath("$.methode").value("CMI"))
                .andExpect(jsonPath("$.avoirPath", startsWith("avoirs/")));

        Payment updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updatedPayment.getStatut()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("Test : remboursement partiel → PARTIALLY_REFUNDED")
    void refundPayment_partialAmount_transitionsToPartiallyRefunded() throws Exception {
        Payment payment = new Payment();
        payment.setCode("PAY-REF200");
        payment.setReservationId(reservation.getId());
        payment.setUserId(client1.getId());
        payment.setMontant(new BigDecimal("1500.00"));
        payment.setDevise("MAD");
        payment.setMethode(PaymentMethod.BANK_TRANSFER);
        payment.setStatut(PaymentStatus.SUCCESS);
        payment = paymentRepository.save(payment);

        // Remboursement partiel de 70% = 1050.00 MAD sur 1500.00 MAD
        RefundRequest partialRequest = new RefundRequest(
                new BigDecimal("1050.00"),
                "Annulation à 5 jours du début (règle 70%)"
        );

        mockMvc.perform(post("/api/admin/payments/" + payment.getId() + "/refund")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agenceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(payment.getId()))
                .andExpect(jsonPath("$.paymentStatut").value("PARTIALLY_REFUNDED"))
                .andExpect(jsonPath("$.montant").value(1050.0))
                .andExpect(jsonPath("$.montantTotalRembourse").value(1050.0))
                .andExpect(jsonPath("$.montantRestant").value(450.0))
                .andExpect(jsonPath("$.statut").value("COMPLETED"))
                .andExpect(jsonPath("$.methode").value("MANUAL"));

        Payment updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updatedPayment.getStatut()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
    }

    @Test
    @DisplayName("Test : montant > paiement (ou > paiement - déjà remboursé) → 400 Bad Request")
    void refundPayment_montantExceedsPayment_returns400() throws Exception {
        Payment payment = new Payment();
        payment.setCode("PAY-REF400");
        payment.setReservationId(reservation.getId());
        payment.setUserId(client1.getId());
        payment.setMontant(new BigDecimal("1000.00"));
        payment.setDevise("MAD");
        payment.setMethode(PaymentMethod.CMI);
        payment.setStatut(PaymentStatus.SUCCESS);
        payment = paymentRepository.save(payment);

        // 1. Montant > paiement (1200.00 > 1000.00) -> 400 Bad Request
        RefundRequest excessiveRequest = new RefundRequest(
                new BigDecimal("1200.00"),
                "Demande supérieure au paiement"
        );

        mockMvc.perform(post("/api/admin/payments/" + payment.getId() + "/refund")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agenceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessiveRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("dépasse le montant remboursable restant")));

        // 2. Premier remboursement partiel valide de 600.00 MAD, puis second de 500.00 MAD (> 400.00 restants) -> 400
        mockMvc.perform(post("/api/admin/payments/" + payment.getId() + "/refund")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agenceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefundRequest(new BigDecimal("600.00"), "1er remboursement"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatut").value("PARTIALLY_REFUNDED"));

        mockMvc.perform(post("/api/admin/payments/" + payment.getId() + "/refund")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agenceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefundRequest(new BigDecimal("500.00"), "2e remboursement excessif"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("400.00")));
    }

    @Test
    @DisplayName("Test : avoir PDF généré et téléchargeable")
    void refundPayment_generatesAvoirPdf() throws Exception {
        Payment payment = new Payment();
        payment.setCode("PAY-REFPDF");
        payment.setReservationId(reservation.getId());
        payment.setUserId(client1.getId());
        payment.setMontant(new BigDecimal("1200.00"));
        payment.setDevise("MAD");
        payment.setMethode(PaymentMethod.CMI);
        payment.setStatut(PaymentStatus.SUCCESS);
        payment = paymentRepository.save(payment);

        RefundRequest refundRequest = new RefundRequest(
                new BigDecimal("600.00"),
                "Remboursement partiel 50% (annulation J-2)"
        );

        mockMvc.perform(post("/api/admin/payments/" + payment.getId() + "/refund")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agenceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avoirPath").isNotEmpty());

        byte[] pdfBytes = mockMvc.perform(get("/api/admin/payments/" + payment.getId() + "/avoir")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agenceToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        try (var pdf = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            String text = new org.apache.pdfbox.text.PDFTextStripper().getText(pdf).replaceAll("\\s+", " ");
            org.assertj.core.api.Assertions.assertThat(text).contains(
                    "LocaVite SARL - Avoir de remboursement",
                    "PAY-REFPDF",
                    "LV-PAYTEST1",
                    "600.00 MAD",
                    "Remboursement partiel 50%"
            );
        }
    }

    @Test
    @DisplayName("Test : notification envoyée (email + SMS) et journalisation complète (audit trail)")
    void refundPayment_sendsClientNotificationsAndLogsAuditTrail() throws Exception {
        long emailCountBefore = emailLogRepository.count();
        long notifCountBefore = notificationLogRepository.count();

        Payment payment = new Payment();
        payment.setCode("PAY-REFNOTIF");
        payment.setReservationId(reservation.getId());
        payment.setUserId(client1.getId());
        payment.setMontant(new BigDecimal("1500.00"));
        payment.setDevise("MAD");
        payment.setMethode(PaymentMethod.CMI);
        payment.setStatut(PaymentStatus.SUCCESS);
        payment = paymentRepository.save(payment);

        RefundRequest refundRequest = new RefundRequest(
                new BigDecimal("1500.00"),
                "Annulation complète selon conditions"
        );

        mockMvc.perform(post("/api/admin/payments/" + payment.getId() + "/refund")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agenceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditTrail", containsString("INITIATED")))
                .andExpect(jsonPath("$.auditTrail", containsString("COMPLETED")));

        org.assertj.core.api.Assertions.assertThat(emailLogRepository.count()).isGreaterThan(emailCountBefore);
        org.assertj.core.api.Assertions.assertThat(notificationLogRepository.count()).isGreaterThan(notifCountBefore);

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getMetadata()).contains("refundHistory", "REFUNDED");
    }
}
