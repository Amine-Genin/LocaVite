package ma.locavite.mail.repository;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.catalog.repository.BrandRepository;
import ma.locavite.catalog.repository.CategoryRepository;
import ma.locavite.mail.domain.EmailLog;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cache.type=none",
        "management.health.mail.enabled=false"
})
@Transactional
class EmailLogRepositoryIT {

    @Autowired
    private EmailLogRepository emailLogRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("Test : persistance et relecture d'un EmailLog avec réservation en base")
    void testSaveAndFindEmailLogWithReservation() {
        User user = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User u = new User();
            u.setNom("El Idrissi");
            u.setPrenom("Youssef");
            u.setEmail("youssef@example.com");
            u.setTelephone("+212600112233");
            u.setPassword("hashedpassword123");
            return userRepository.save(u);
        });

        Brand brand = brandRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Brand b = new Brand();
                    b.setName("Brand_" + System.currentTimeMillis());
                    return brandRepository.save(b);
                });

        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setName("Category_" + System.currentTimeMillis());
                    c.setCaution(new BigDecimal("2000.00"));
                    return categoryRepository.save(c);
                });

        Vehicle vehicle = vehicleRepository.findAll().stream().findFirst().orElseGet(() -> {
            Vehicle v = new Vehicle();
            v.setBrand(brand);
            v.setCategory(category);
            v.setModele("Logan");
            v.setMatricule("99999-B-1");
            v.setPrixJour(new BigDecimal("250.00"));
            v.setPrixVente(new BigDecimal("150000.00"));
            v.setAnnee(2023);
            v.setAgenceId(user.getId());
            v.setPlaces(5);
            v.setDescription("Super véhicule");
            v.setCarburant(ma.locavite.vehicle.domain.FuelType.ESSENCE);
            v.setTransmission(ma.locavite.vehicle.domain.TransmissionType.MANUELLE);
            v.setStatut(ma.locavite.vehicle.domain.VehicleStatus.PUBLISHED);
            v.setVille("Casablanca");
            return vehicleRepository.save(v);
        });

        Reservation reservation = new Reservation();
        reservation.setCode("LV-LOG-001");
        reservation.setUserId(user.getId());
        reservation.setVehicleId(vehicle.getId());
        reservation.setDateDebut(LocalDate.now().plusDays(1));
        reservation.setDateFin(LocalDate.now().plusDays(4));
        reservation.setLieuPriseEnCharge("Rabat");
        reservation.setLieuRetour("Rabat");
        reservation.setPrixTotal(new BigDecimal("1000.00"));
        reservation.setCaution(new BigDecimal("2000.00"));
        reservation.setStatut(ReservationStatus.CONFIRMED);
        reservation = reservationRepository.save(reservation);

        EmailLog log = new EmailLog(
                reservation.getId(),
                "youssef@example.com",
                "Confirmation de réservation - LV-LOG-001",
                "mail/reservation-confirmation-fr",
                "SENT",
                null,
                1
        );

        EmailLog saved = emailLogRepository.save(log);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        List<EmailLog> logs = emailLogRepository.findByReservationId(reservation.getId());
        assertThat(logs).hasSize(1);
        EmailLog found = logs.get(0);

        assertThat(found.getRecipient()).isEqualTo("youssef@example.com");
        assertThat(found.getDestinataire()).isEqualTo("youssef@example.com");
        assertThat(found.getSubject()).isEqualTo("Confirmation de réservation - LV-LOG-001");
        assertThat(found.getSujet()).isEqualTo("Confirmation de réservation - LV-LOG-001");
        assertThat(found.getStatus()).isEqualTo("SENT");
        assertThat(found.getStatut()).isEqualTo("SENT");
        assertThat(found.getAttempts()).isEqualTo(1);
        assertThat(found.getTentatives()).isEqualTo(1);
        assertThat(found.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Test : persistance d'un EmailLog sans réservation (ex: email général)")
    void testSaveEmailLogWithoutReservation() {
        EmailLog log = new EmailLog(
                null,
                "general@test.com",
                "Message de test",
                "mail/test",
                "SENT",
                null,
                1
        );

        EmailLog saved = emailLogRepository.save(log);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getReservationId()).isNull();

        List<EmailLog> logs = emailLogRepository.findByRecipient("general@test.com");
        assertThat(logs).isNotEmpty();
    }
}
