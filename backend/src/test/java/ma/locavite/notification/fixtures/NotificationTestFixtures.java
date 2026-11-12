package ma.locavite.notification.fixtures;

import ma.locavite.auth.domain.User;
import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.mail.fixtures.EmailTestFixtures;
import ma.locavite.notification.domain.NotificationLog;
import ma.locavite.notification.domain.NotificationPreference;
import ma.locavite.notification.domain.NotificationType;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.vehicle.domain.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class NotificationTestFixtures {

    private NotificationTestFixtures() {
    }

    public static User createClient(Long id, String phone, NotificationPreference preference, boolean optIn) {
        User user = EmailTestFixtures.createClient(id, "client" + id + "@example.com", "Alaoui", "Fatima");
        user.setTelephone(phone);
        user.setPreferenceNotification(preference);
        user.setNotificationOptIn(optIn);
        return user;
    }

    public static Vehicle createVehicle() {
        Brand brand = EmailTestFixtures.createBrand("Toyota");
        Category category = EmailTestFixtures.createCategory("SUV", new BigDecimal("3000.00"));
        return EmailTestFixtures.createVehicle(brand, category);
    }

    public static Reservation createReservation(Long id, String code, User client, Vehicle vehicle,
                                                LocalDate debut, LocalDate fin, ReservationStatus statut) {
        Reservation reservation = EmailTestFixtures.createReservation(id, code, client, vehicle);
        reservation.setDateDebut(debut);
        reservation.setDateFin(fin);
        reservation.setStatut(statut);
        return reservation;
    }

    public static NotificationLog createNotificationLog(Long reservationId, Long userId, String recipient,
                                                        String channel, NotificationType type, String status) {
        return new NotificationLog(
                reservationId,
                userId,
                recipient,
                channel,
                type.name(),
                "Message de notification pour test",
                status,
                null,
                1
        );
    }
}
