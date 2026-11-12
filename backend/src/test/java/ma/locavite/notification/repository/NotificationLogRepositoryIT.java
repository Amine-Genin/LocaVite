package ma.locavite.notification.repository;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.catalog.repository.BrandRepository;
import ma.locavite.catalog.repository.CategoryRepository;
import ma.locavite.mail.fixtures.EmailTestFixtures;
import ma.locavite.notification.domain.NotificationLog;
import ma.locavite.notification.domain.NotificationType;
import ma.locavite.notification.fixtures.NotificationTestFixtures;
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
class NotificationLogRepositoryIT {

    @Autowired
    private NotificationLogRepository notificationLogRepository;

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
        notificationLogRepository.deleteAll();
        reservationRepository.deleteAll();
    }

    @Test
    @DisplayName("Test : persistance et relecture d'un NotificationLog avec réservation et client")
    void testSaveAndFindNotificationLog() {
        User user = userRepository.findAll().stream().findFirst().orElseGet(() ->
                userRepository.save(EmailTestFixtures.createClient(null, "noti.client@example.com", "Berrada", "Salma")));

        Brand brand = brandRepository.findAll().stream().findFirst().orElseGet(() ->
                brandRepository.save(EmailTestFixtures.createBrand("BrandNotif_" + System.currentTimeMillis())));

        Category category = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(EmailTestFixtures.createCategory("CategoryNotif_" + System.currentTimeMillis(), new BigDecimal("2000.00"))));

        Vehicle vehicle = vehicleRepository.findAll().stream().findFirst().orElseGet(() ->
                vehicleRepository.save(EmailTestFixtures.createVehicle(null, brand, category, "88888-A-1", user.getId(), new BigDecimal("350.00"))));

        Reservation reservation = reservationRepository.save(
                EmailTestFixtures.createReservation(null, "LV-NOTIF-01", user, vehicle));

        NotificationLog log = NotificationTestFixtures.createNotificationLog(
                reservation.getId(), user.getId(), "+212611223344", "SMS", NotificationType.CONFIRMATION, "SENT");

        NotificationLog saved = notificationLogRepository.save(log);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        List<NotificationLog> logs = notificationLogRepository.findByReservationId(reservation.getId());
        assertThat(logs).hasSize(1);

        NotificationLog found = logs.get(0);
        assertThat(found.getRecipient()).isEqualTo("+212611223344");
        assertThat(found.getDestinataire()).isEqualTo("+212611223344");
        assertThat(found.getChannel()).isEqualTo("SMS");
        assertThat(found.getCanal()).isEqualTo("SMS");
        assertThat(found.getNotificationType()).isEqualTo("CONFIRMATION");
        assertThat(found.getType()).isEqualTo("CONFIRMATION");
        assertThat(found.getStatus()).isEqualTo("SENT");
        assertThat(found.getStatut()).isEqualTo("SENT");
        assertThat(found.getAttempts()).isEqualTo(1);
        assertThat(found.getTentatives()).isEqualTo(1);

        boolean exists = notificationLogRepository.existsByReservationIdAndNotificationTypeAndStatus(
                reservation.getId(), "CONFIRMATION", "SENT");
        assertThat(exists).isTrue();
    }
}
