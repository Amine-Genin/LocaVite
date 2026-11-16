package ma.locavite.auth.config;

import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.RoleType;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.RoleRepository;
import ma.locavite.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Component
@Order(10)
@ConditionalOnProperty(name = "locavite.demo.seed-enabled", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    public static final String DEMO_PASSWORD = "Demo2026!";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DemoDataInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Integer existingDemoCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations WHERE code LIKE 'RES-DEMO-%'",
                Integer.class
        );
        if (existingDemoCount != null && existingDemoCount > 0) {
            log.info("Données de démonstration déjà présentes ({} réservations RES-DEMO-*).", existingDemoCount);
            return;
        }

        log.info("Initialisation des données de démonstration LocaVite...");

        Role adminRole = getOrCreateRole(RoleType.ADMIN.name());
        Role agenceRole = getOrCreateRole(RoleType.AGENCE.name());
        Role clientRole = getOrCreateRole(RoleType.CLIENT.name());

        User admin = getOrCreateUser(
                "Admin", "Dev",
                DevAdminInitializer.DEV_ADMIN_EMAIL,
                "+212600000000",
                DevAdminInitializer.DEV_ADMIN_PASSWORD,
                adminRole
        );

        User agence = getOrCreateUser(
                "Atlas Auto", "Agence",
                "agence.casa@locavite.ma",
                "+212661112233",
                DEMO_PASSWORD,
                agenceRole
        );

        User client1 = getOrCreateUser(
                "El Amrani", "Youssef",
                "client.demo@locavite.ma",
                "+212662334455",
                DEMO_PASSWORD,
                clientRole
        );

        User client2 = getOrCreateUser(
                "Benali", "Salma",
                "salma.benali@locavite.ma",
                "+212663445566",
                DEMO_PASSWORD,
                clientRole
        );

        User client3 = getOrCreateUser(
                "Tazi", "Karim",
                "karim.tazi@locavite.ma",
                "+212664556677",
                DEMO_PASSWORD,
                clientRole
        );

        seedDocuments(client1.getId(), client2.getId(), admin.getId());

        Long v1 = getOrCreateVehicle(
                "Dacia", "Sandero Stepway", 2024, "CITADINE", "Casablanca",
                280.00, 165000.00, "DIESEL", "MANUELLE", 5,
                "Citadine polyvalente et économique, idéale pour vos déplacements à Casablanca et régions. Climatisation, Bluetooth, radar de recul.",
                "12345-A-1", "PUBLISHED", agence.getId(),
                "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=1000&q=80"
        );

        Long v2 = getOrCreateVehicle(
                "Renault", "Clio 5 Alpine", 2024, "CITADINE", "Rabat",
                320.00, 195000.00, "ESSENCE", "AUTOMATIQUE", 5,
                "Renault Clio 5 finition Esprit Alpine boîte automatique, écran tactile CarPlay/Android Auto et caméra 360°.",
                "23456-B-2", "PUBLISHED", agence.getId(),
                "https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?auto=format&fit=crop&w=1000&q=80"
        );

        Long v3 = getOrCreateVehicle(
                "Peugeot", "3008 GT", 2023, "SUV", "Marrakech",
                550.00, 340000.00, "DIESEL", "AUTOMATIQUE", 5,
                "SUV familial grand confort avec i-Cockpit, toit panoramique ouvrant et aides à la conduite avancées.",
                "34567-D-26", "PUBLISHED", agence.getId(),
                "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?auto=format&fit=crop&w=1000&q=80"
        );

        Long v4 = getOrCreateVehicle(
                "Volkswagen", "Golf 8 R-Line", 2024, "BERLINE", "Casablanca",
                480.00, 320000.00, "DIESEL", "AUTOMATIQUE", 5,
                "Compacte premium dynamique, boîte DSG7, Digital Cockpit Pro et phares IQ.Light Matrix LED.",
                "45678-A-6", "PUBLISHED", agence.getId(),
                "https://images.unsplash.com/photo-1609521263047-f8f205293f24?auto=format&fit=crop&w=1000&q=80"
        );

        Long v5 = getOrCreateVehicle(
                "Hyundai", "Tucson Hybrid", 2024, "SUV", "Tanger",
                520.00, 360000.00, "HYBRIDE", "AUTOMATIQUE", 5,
                "SUV hybride moderne et silencieux, très faible consommation en ville comme sur autoroute.",
                "56789-E-40", "PUBLISHED", agence.getId(),
                "https://images.unsplash.com/photo-1519641471654-76ce0107ad1b?auto=format&fit=crop&w=1000&q=80"
        );

        Long v6 = getOrCreateVehicle(
                "Mercedes", "Classe C 220d AMG", 2024, "LUXE", "Casablanca",
                950.00, 580000.00, "DIESEL", "AUTOMATIQUE", 5,
                "Berline de prestige Pack AMG Line, sellerie cuir Nappa, système MBUX grand écran et sonorisation Burmester.",
                "67890-A-1", "PUBLISHED", agence.getId(),
                "https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?auto=format&fit=crop&w=1000&q=80"
        );

        Long v7 = getOrCreateVehicle(
                "Toyota", "Corolla Hybrid", 2023, "BERLINE", "Agadir",
                400.00, 260000.00, "HYBRIDE", "AUTOMATIQUE", 5,
                "Berline hybride fiable et spacieuse avec grand coffre, parfaite pour les longs trajets côtiers.",
                "78901-C-33", "PUBLISHED", agence.getId(),
                "https://images.unsplash.com/photo-1621007947382-bb3c3994e3fb?auto=format&fit=crop&w=1000&q=80"
        );

        Long v8 = getOrCreateVehicle(
                "BMW", "X5 xDrive30d", 2024, "LUXE", "Marrakech",
                1400.00, 890000.00, "DIESEL", "AUTOMATIQUE", 5,
                "SUV de luxe 4 roues motrices xDrive, suspension pneumatique et finition M Sport.",
                "89012-D-26", "PUBLISHED", agence.getId(),
                "https://images.unsplash.com/photo-1555215695-3004980ad54e?auto=format&fit=crop&w=1000&q=80"
        );

        LocalDate today = LocalDate.now();
        Instant now = Instant.now();

        // 1. COMPLETED reservation #1 (30 to 25 days ago)
        Long r1 = insertReservation(
                "RES-DEMO-001", client1.getId(), v1,
                today.minusDays(30), today.minusDays(25),
                "Casablanca - Aéroport Mohammed V", "Casablanca - Aéroport Mohammed V",
                1400.00, 250.00, 0.00, 0.00, 1650.00, 3000.00,
                "COMPLETED", "RELEASED",
                now.minus(32, ChronoUnit.DAYS)
        );
        insertOption(r1, "GPS");
        insertCautionTx(r1, 3000.00, "HOLD", "HELD", "Empreinte bancaire caution prise au départ", now.minus(30, ChronoUnit.DAYS));
        insertCautionTx(r1, 3000.00, "RELEASE", "RELEASED", "Véhicule restitué en parfait état, caution libérée", now.minus(25, ChronoUnit.DAYS));
        Long p1 = insertPayment(
                "PAY-DEMO-001", r1, client1.getId(), 1650.00, "CMI", "SUCCESS",
                "CMI-REF-2026-001", "TXN-CMI-981001",
                now.minus(31, ChronoUnit.DAYS), now.minus(31, ChronoUnit.DAYS)
        );
        insertInvoice("FAC-2026-0001", p1, 1375.00, 275.00, 1650.00, "invoices/FAC-2026-0001.pdf", now.minus(31, ChronoUnit.DAYS));

        // 2. COMPLETED reservation #2 with extension & partial refund (18 to 14 days ago)
        Long r2 = insertReservation(
                "RES-DEMO-002", client1.getId(), v3,
                today.minusDays(18), today.minusDays(14),
                "Marrakech - Agence Guéliz", "Marrakech - Aéroport Ménara",
                2200.00, 120.00, 0.00, 0.00, 2320.00, 4000.00,
                "COMPLETED", "PARTIALLY_RELEASED",
                now.minus(20, ChronoUnit.DAYS)
        );
        insertOption(r2, "BABY_SEAT");
        jdbcTemplate.update(
                "INSERT INTO reservation_extensions (reservation_id, ancienne_date_fin, nouvelle_date_fin, prix_supplementaire, created_at) VALUES (?, ?, ?, ?, ?)",
                r2, Date.valueOf(today.minusDays(15)), Date.valueOf(today.minusDays(14)), 550.00, Timestamp.from(now.minus(16, ChronoUnit.DAYS))
        );
        insertCautionTx(r2, 4000.00, "HOLD", "HELD", "Caution bloquée au départ à Marrakech Guéliz", now.minus(18, ChronoUnit.DAYS));
        insertCautionTx(r2, 3700.00, "RELEASE", "PARTIALLY_RELEASED", "Retenue de 300 MAD pour nettoyage approfondi", now.minus(14, ChronoUnit.DAYS));
        Long p2 = insertPayment(
                "PAY-DEMO-002", r2, client1.getId(), 2320.00, "CMI", "PARTIALLY_REFUNDED",
                "CMI-REF-2026-002", "TXN-CMI-981002",
                now.minus(19, ChronoUnit.DAYS), now.minus(19, ChronoUnit.DAYS)
        );
        insertInvoice("FAC-2026-0002", p2, 1933.33, 386.67, 2320.00, "invoices/FAC-2026-0002.pdf", now.minus(19, ChronoUnit.DAYS));
        insertRefund(p2, r2, 250.00, "Geste commercial suite à attente en agence", "COMPLETED", "CMI", "REF-CMI-002", now.minus(13, ChronoUnit.DAYS));

        // 3. ACTIVE reservation (started 2 days ago, ends in 3 days)
        Long r3 = insertReservation(
                "RES-DEMO-003", client1.getId(), v4,
                today.minusDays(2), today.plusDays(3),
                "Casablanca - Gare Casa-Port", "Casablanca - Gare Casa-Port",
                2400.00, 750.00, 0.00, 0.00, 3150.00, 3500.00,
                "ACTIVE", "HELD",
                now.minus(4, ChronoUnit.DAYS)
        );
        insertOption(r3, "GPS");
        insertOption(r3, "EXTRA_DRIVER");
        insertCautionTx(r3, 3500.00, "HOLD", "HELD", "Caution active pendant la location en cours", now.minus(2, ChronoUnit.DAYS));
        Long p3 = insertPayment(
                "PAY-DEMO-003", r3, client1.getId(), 3150.00, "CMI", "SUCCESS",
                "CMI-REF-2026-003", "TXN-CMI-981003",
                now.minus(4, ChronoUnit.DAYS), now.minus(4, ChronoUnit.DAYS)
        );
        insertInvoice("FAC-2026-0003", p3, 2625.00, 525.00, 3150.00, "invoices/FAC-2026-0003.pdf", now.minus(4, ChronoUnit.DAYS));

        // 4. CONFIRMED upcoming reservation (starts in 5 days, ends in 9 days)
        Long r4 = insertReservation(
                "RES-DEMO-004", client1.getId(), v6,
                today.plusDays(5), today.plusDays(9),
                "Casablanca - Quartier Anfa", "Casablanca - Aéroport Mohammed V",
                3800.00, 0.00, 0.00, 0.00, 3800.00, 5000.00,
                "CONFIRMED", "HELD",
                now.minus(2, ChronoUnit.DAYS)
        );
        Long p4 = insertPayment(
                "PAY-DEMO-004", r4, client1.getId(), 3800.00, "CMI", "SUCCESS",
                "CMI-REF-2026-004", "TXN-CMI-981004",
                now.minus(2, ChronoUnit.DAYS), now.minus(2, ChronoUnit.DAYS)
        );
        insertInvoice("FAC-2026-0004", p4, 3166.67, 633.33, 3800.00, "invoices/FAC-2026-0004.pdf", now.minus(2, ChronoUnit.DAYS));

        // 5. PENDING upcoming reservation for Client 1 (ready to pay / confirm!)
        Long r5 = insertReservation(
                "RES-DEMO-005", client1.getId(), v2,
                today.plusDays(12), today.plusDays(15),
                "Rabat - Agence Agdal", "Rabat - Gare Rabat-Ville",
                960.00, 90.00, 0.00, 0.00, 1050.00, 3000.00,
                "PENDING", "PENDING",
                now.minus(1, ChronoUnit.DAYS)
        );
        insertOption(r5, "BABY_SEAT");
        insertPayment(
                "PAY-DEMO-005", r5, client1.getId(), 1050.00, "CMI", "PENDING",
                "CMI-REF-2026-005", null,
                now.minus(1, ChronoUnit.DAYS), null
        );

        // 6. PENDING upcoming reservation for Client 2 (Salma Benali)
        Long r6 = insertReservation(
                "RES-DEMO-006", client2.getId(), v5,
                today.plusDays(7), today.plusDays(11),
                "Tanger - Gare Boraq TGV", "Tanger - Aéroport Ibn Battouta",
                2080.00, 200.00, 0.00, 0.00, 2280.00, 3500.00,
                "PENDING", "PENDING",
                now.minus(12, ChronoUnit.HOURS)
        );
        insertOption(r6, "GPS");
        insertPayment(
                "PAY-DEMO-006", r6, client2.getId(), 2280.00, "CASH_AGENCY", "PENDING",
                "AGENCE-REF-2026-006", null,
                now.minus(12, ChronoUnit.HOURS), null
        );

        // 7. CANCELLED reservation with full refund (Client 1)
        Long r7 = insertReservation(
                "RES-DEMO-007", client1.getId(), v7,
                today.minusDays(10), today.minusDays(7),
                "Agadir - Marina", "Agadir - Aéroport Al Massira",
                1200.00, 0.00, 0.00, 0.00, 1200.00, 3000.00,
                "CANCELLED", "RELEASED",
                now.minus(14, ChronoUnit.DAYS)
        );
        Long p7 = insertPayment(
                "PAY-DEMO-007", r7, client1.getId(), 1200.00, "CMI", "REFUNDED",
                "CMI-REF-2026-007", "TXN-CMI-981007",
                now.minus(14, ChronoUnit.DAYS), now.minus(14, ChronoUnit.DAYS)
        );
        insertInvoice("FAC-2026-0005", p7, 1000.00, 200.00, 1200.00, "invoices/FAC-2026-0005.pdf", now.minus(14, ChronoUnit.DAYS));
        insertRefund(p7, r7, 1200.00, "Annulation client plus de 48h avant le départ (remboursement intégral)", "COMPLETED", "CMI", "REF-CMI-007", now.minus(12, ChronoUnit.DAYS));

        // 8. CANCELLED reservation with failed payment (Client 3)
        Long r8 = insertReservation(
                "RES-DEMO-008", client3.getId(), v8,
                today.minusDays(5), today.minusDays(2),
                "Marrakech - Aéroport Ménara", "Marrakech - Aéroport Ménara",
                4200.00, 0.00, 0.00, 0.00, 4200.00, 5000.00,
                "CANCELLED", "PENDING",
                now.minus(6, ChronoUnit.DAYS)
        );
        insertPayment(
                "PAY-DEMO-008", r8, client3.getId(), 4200.00, "CMI", "FAILED",
                "CMI-REF-2026-008", "TXN-CMI-ERR-008",
                now.minus(6, ChronoUnit.DAYS), null
        );

        // Email & SMS notification logs for history
        insertEmailLog(r1, client1.getEmail(), "Confirmation de votre réservation RES-DEMO-001", "reservation-confirmed", "SENT", now.minus(31, ChronoUnit.DAYS));
        insertEmailLog(r3, client1.getEmail(), "Confirmation et facture FAC-2026-0003 - Réservation RES-DEMO-003", "payment-receipt", "SENT", now.minus(4, ChronoUnit.DAYS));
        insertEmailLog(r4, client1.getEmail(), "Confirmation de votre réservation RES-DEMO-004", "reservation-confirmed", "SENT", now.minus(2, ChronoUnit.DAYS));
        insertEmailLog(r7, client1.getEmail(), "Confirmation de remboursement - Réservation RES-DEMO-007", "refund-processed", "SENT", now.minus(12, ChronoUnit.DAYS));

        insertNotificationLog(r3, client1.getId(), client1.getTelephone(), "SMS", "PAYMENT_CONFIRMED", "LocaVite: Paiement de 3150.00 MAD reçu pour votre réservation RES-DEMO-003.", "SENT", now.minus(4, ChronoUnit.DAYS));
        insertNotificationLog(r4, client1.getId(), client1.getTelephone(), "WHATSAPP", "RESERVATION_CONFIRMED", "LocaVite: Votre réservation RES-DEMO-004 (Mercedes Classe C) est confirmée.", "SENT", now.minus(2, ChronoUnit.DAYS));

        log.info("Données de démonstration initialisées avec succès (8 véhicules, 8 réservations, paiements, factures et historiques) !");
    }

    private Role getOrCreateRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
    }

    private User getOrCreateUser(String nom, String prenom, String email, String telephone, String rawPassword, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = User.builder()
                    .nom(nom)
                    .prenom(prenom)
                    .email(email)
                    .telephone(telephone)
                    .password(passwordEncoder.encode(rawPassword))
                    .roles(Set.of(role))
                    .build();
            return userRepository.save(u);
        });
    }

    private void seedDocuments(Long client1Id, Long client2Id, Long adminId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM documents WHERE user_id = ?",
                Integer.class,
                client1Id
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO documents (user_id, type, path, status, uploaded_at, verified_at, verified_by) VALUES (?, 'PERMIS', ?, 'VALIDATED', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '19 days', ?)",
                client1Id, "documents/demo/permis-youssef.pdf", adminId
        );
        jdbcTemplate.update(
                "INSERT INTO documents (user_id, type, path, status, uploaded_at, verified_at, verified_by) VALUES (?, 'CIN', ?, 'VALIDATED', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '19 days', ?)",
                client1Id, "documents/demo/cin-youssef.pdf", adminId
        );
        jdbcTemplate.update(
                "INSERT INTO documents (user_id, type, path, status, uploaded_at) VALUES (?, 'PERMIS', ?, 'PENDING', CURRENT_TIMESTAMP - INTERVAL '1 day')",
                client2Id, "documents/demo/permis-salma.pdf"
        );
    }

    private Long getOrCreateVehicle(
            String brandName, String modele, int annee, String categoryName, String ville,
            double prixJour, double prixVente, String carburant, String transmission, int places,
            String description, String matricule, String statut, Long agenceId, String photoUrl
    ) {
        var existing = jdbcTemplate.queryForList(
                "SELECT id FROM vehicles WHERE matricule = ?",
                Long.class,
                matricule
        );
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        Long brandId = jdbcTemplate.queryForObject(
                "SELECT id FROM brands WHERE LOWER(name) = LOWER(?)",
                Long.class,
                brandName
        );
        Long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM categories WHERE UPPER(name) = UPPER(?)",
                Long.class,
                categoryName
        );

        jdbcTemplate.update(
                """
                INSERT INTO vehicles (brand_id, modele, annee, category_id, ville, prix_jour, prix_vente, carburant, transmission, places, description, matricule, statut, agence_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                brandId, modele, annee, categoryId, ville, prixJour, prixVente,
                carburant, transmission, places, description, matricule, statut, agenceId
        );

        Long vehicleId = jdbcTemplate.queryForObject(
                "SELECT id FROM vehicles WHERE matricule = ?",
                Long.class,
                matricule
        );

        jdbcTemplate.update(
                "INSERT INTO vehicle_photos (vehicle_id, path, thumbnail_path, is_primary, ordre) VALUES (?, ?, ?, true, 0)",
                vehicleId, photoUrl + "&v=" + matricule, photoUrl + "&thumb=" + matricule
        );

        return vehicleId;
    }

    private Long insertReservation(
            String code, Long userId, Long vehicleId,
            LocalDate dateDebut, LocalDate dateFin,
            String lieuPrise, String lieuRetour,
            double sousTotal, double options, double remise, double tva, double total, double caution,
            String statut, String cautionStatut, Instant createdAt
    ) {
        Timestamp ts = Timestamp.from(createdAt);
        jdbcTemplate.update(
                """
                INSERT INTO reservations (code, user_id, vehicle_id, date_debut, date_fin, lieu_prise_en_charge, lieu_retour,
                    prix_sous_total, prix_options, prix_remise, prix_tva, prix_total, caution, statut, caution_statut, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                code, userId, vehicleId, Date.valueOf(dateDebut), Date.valueOf(dateFin),
                lieuPrise, lieuRetour, sousTotal, options, remise, tva, total, caution,
                statut, cautionStatut, ts, ts
        );
        return jdbcTemplate.queryForObject("SELECT id FROM reservations WHERE code = ?", Long.class, code);
    }

    private void insertOption(Long reservationId, String optionName) {
        jdbcTemplate.update(
                "INSERT INTO reservation_options (reservation_id, option_name) VALUES (?, ?)",
                reservationId, optionName
        );
    }

    private void insertCautionTx(Long reservationId, double montant, String type, String statut, String note, Instant dateOp) {
        jdbcTemplate.update(
                "INSERT INTO caution_transactions (reservation_id, montant, type, statut, date_operation, note) VALUES (?, ?, ?, ?, ?, ?)",
                reservationId, montant, type, statut, Timestamp.from(dateOp), note
        );
    }

    private Long insertPayment(
            String code, Long reservationId, Long userId, double montant,
            String methode, String statut, String reference, String transactionId,
            Instant createdAt, Instant paidAt
    ) {
        Timestamp createdTs = Timestamp.from(createdAt);
        Timestamp paidTs = paidAt != null ? Timestamp.from(paidAt) : null;
        jdbcTemplate.update(
                """
                INSERT INTO payments (code, reservation_id, user_id, montant, devise, methode, statut, reference, transaction_id, created_at, updated_at, paid_at)
                VALUES (?, ?, ?, ?, 'MAD', ?, ?, ?, ?, ?, ?, ?)
                """,
                code, reservationId, userId, montant, methode, statut, reference, transactionId, createdTs, createdTs, paidTs
        );
        return jdbcTemplate.queryForObject("SELECT id FROM payments WHERE code = ?", Long.class, code);
    }

    private void insertInvoice(String numero, Long paymentId, double ht, double tva, double ttc, String pdfPath, Instant dateEmission) {
        jdbcTemplate.update(
                """
                INSERT INTO invoices (numero, payment_id, montant_ht, montant_tva, montant_ttc, chemin_pdf, date_emission)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                numero, paymentId, ht, tva, ttc, pdfPath, Timestamp.from(dateEmission)
        );
    }

    private void insertRefund(Long paymentId, Long reservationId, double montant, String raison, String statut, String methode, String reference, Instant dateOp) {
        jdbcTemplate.update(
                """
                INSERT INTO refunds (payment_id, reservation_id, montant, raison, statut, methode, reference, date_operation)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                paymentId, reservationId, montant, raison, statut, methode, reference, Timestamp.from(dateOp)
        );
    }

    private void insertEmailLog(Long reservationId, String recipient, String subject, String template, String status, Instant createdAt) {
        jdbcTemplate.update(
                """
                INSERT INTO email_logs (reservation_id, recipient, destinataire, subject, sujet, template_name, status, statut, attempts, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?)
                """,
                reservationId, recipient, recipient, subject, subject, template, status, status, Timestamp.from(createdAt)
        );
    }

    private void insertNotificationLog(Long reservationId, Long userId, String recipient, String channel, String type, String content, String status, Instant createdAt) {
        Timestamp ts = Timestamp.from(createdAt);
        jdbcTemplate.update(
                """
                INSERT INTO notification_logs (reservation_id, user_id, recipient, destinataire, channel, canal, notification_type, type, content, message, status, statut, attempts, tentatives, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, ?, ?)
                """,
                reservationId, userId, recipient, recipient, channel, channel, type, type, content, content, status, status, ts, ts
        );
    }
}
