package ma.locavite.mail.repository;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.catalog.repository.BrandRepository;
import ma.locavite.catalog.repository.CategoryRepository;
import ma.locavite.mail.domain.EmailLog;
import ma.locavite.mail.fixtures.EmailTestFixtures;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @AfterEach
    void tearDown() {
        emailLogRepository.deleteAll();
        reservationRepository.deleteAll();
    }

    @Test
    @DisplayName("Test : persistance et relecture d'un EmailLog avec réservation en base")
    void testSaveAndFindEmailLogWithReservation() {
        User user = userRepository.findAll().stream().findFirst().orElseGet(() ->
                userRepository.save(EmailTestFixtures.createClient(null, "youssef@example.com", "El Idrissi", "Youssef")));

        Brand brand = brandRepository.findAll().stream().findFirst().orElseGet(() ->
                brandRepository.save(EmailTestFixtures.createBrand("Brand_" + System.currentTimeMillis())));

        Category category = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(EmailTestFixtures.createCategory("Category_" + System.currentTimeMillis(), new BigDecimal("2000.00"))));

        Vehicle vehicle = vehicleRepository.findAll().stream().findFirst().orElseGet(() ->
                vehicleRepository.save(EmailTestFixtures.createVehicle(null, brand, category, "99999-B-1", user.getId(), new BigDecimal("250.00"))));

        Reservation reservation = reservationRepository.save(
                EmailTestFixtures.createReservation(null, "LV-LOG-001", user, vehicle));

        EmailLog log = EmailTestFixtures.createEmailLog(reservation.getId(), "youssef@example.com", "SENT");
        EmailLog saved = emailLogRepository.save(log);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        List<EmailLog> logs = emailLogRepository.findByReservationId(reservation.getId());
        assertThat(logs).hasSize(1);
        EmailLog found = logs.get(0);

        assertThat(found.getRecipient()).isEqualTo("youssef@example.com");
        assertThat(found.getDestinataire()).isEqualTo("youssef@example.com");
        assertThat(found.getSubject()).isEqualTo("Confirmation de réservation");
        assertThat(found.getSujet()).isEqualTo("Confirmation de réservation");
        assertThat(found.getStatus()).isEqualTo("SENT");
        assertThat(found.getStatut()).isEqualTo("SENT");
        assertThat(found.getAttempts()).isEqualTo(1);
        assertThat(found.getTentatives()).isEqualTo(1);
        assertThat(found.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Test : persistance d'un EmailLog sans réservation (ex: email général)")
    void testSaveEmailLogWithoutReservation() {
        EmailLog log = EmailTestFixtures.createEmailLog(null, "general@test.com", "SENT");

        EmailLog saved = emailLogRepository.save(log);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getReservationId()).isNull();

        List<EmailLog> logs = emailLogRepository.findByRecipient("general@test.com");
        assertThat(logs).isNotEmpty();
    }
}
