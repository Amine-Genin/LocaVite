package ma.locavite.notification.service;

import ma.locavite.auth.domain.User;
import ma.locavite.notification.domain.NotificationPreference;
import ma.locavite.notification.domain.NotificationType;
import ma.locavite.notification.fixtures.NotificationTestFixtures;
import ma.locavite.reservation.domain.ContractLanguage;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.vehicle.domain.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTemplateTest {

    private NotificationTemplateService templateService;
    private User client;
    private Vehicle vehicle;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        templateService = new NotificationTemplateService();
        client = NotificationTestFixtures.createClient(1L, "0612345678", NotificationPreference.SMS, true);
        vehicle = NotificationTestFixtures.createVehicle();
        reservation = NotificationTestFixtures.createReservation(
                100L,
                "LV-2026-TEST",
                client,
                vehicle,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5),
                ReservationStatus.CONFIRMED
        );
    }

    @Test
    @DisplayName("Test template : confirmation en français contient le code, le véhicule, les dates et le lieu")
    void confirmationFrenchTemplateContainsExpectedDetails() {
        String message = templateService.buildMessage(
                NotificationType.CONFIRMATION, ContractLanguage.FR, reservation, vehicle, client);

        assertThat(message)
                .contains("LocaVite")
                .contains("LV-2026-TEST")
                .contains("Toyota RAV4")
                .contains("01/12/2026")
                .contains("05/12/2026")
                .contains("Casablanca")
                .contains("confirmée");
    }

    @Test
    @DisplayName("Test template : confirmation en arabe contient les détails en arabe")
    void confirmationArabicTemplateContainsArabicText() {
        String message = templateService.buildMessage(
                NotificationType.CONFIRMATION, ContractLanguage.AR, reservation, vehicle, client);

        assertThat(message)
                .contains("لوكا فيت")
                .contains("LV-2026-TEST")
                .contains("Toyota RAV4")
                .contains("01/12/2026")
                .contains("05/12/2026")
                .contains("Casablanca")
                .contains("تأكيد");
    }

    @Test
    @DisplayName("Test template : rappel J-1 en français et en arabe")
    void reminderJ1TemplateContainsExpectedDetails() {
        String fr = templateService.buildMessage(
                NotificationType.REMINDER_J1, ContractLanguage.FR, reservation, vehicle, client);
        assertThat(fr)
                .contains("Rappel")
                .contains("LV-2026-TEST")
                .contains("01/12/2026")
                .contains("permis");

        String ar = templateService.buildMessage(
                NotificationType.REMINDER_J1, ContractLanguage.AR, reservation, vehicle, client);
        assertThat(ar)
                .contains("تذكير")
                .contains("LV-2026-TEST")
                .contains("01/12/2026")
                .contains("رخصة القيادة");
    }

    @Test
    @DisplayName("Test template : fin de location (retour) en français et en arabe")
    void completionTemplateContainsExpectedDetails() {
        String fr = templateService.buildMessage(
                NotificationType.COMPLETION, ContractLanguage.FR, reservation, vehicle, client);
        assertThat(fr)
                .contains("terminée")
                .contains("LV-2026-TEST");

        String ar = templateService.buildMessage(
                NotificationType.COMPLETION, ContractLanguage.AR, reservation, vehicle, client);
        assertThat(ar)
                .contains("انتهت")
                .contains("LV-2026-TEST");
    }

    @Test
    @DisplayName("Test template : annulation en français et en arabe")
    void cancellationTemplateContainsExpectedDetails() {
        String fr = templateService.buildMessage(
                NotificationType.CANCELLATION, ContractLanguage.FR, reservation, vehicle, client);
        assertThat(fr)
                .contains("annulée")
                .contains("LV-2026-TEST");

        String ar = templateService.buildMessage(
                NotificationType.CANCELLATION, ContractLanguage.AR, reservation, vehicle, client);
        assertThat(ar)
                .contains("إلغاء")
                .contains("LV-2026-TEST");
    }

    @Test
    @DisplayName("Test normalisation téléphone marocain au format international E.164")
    void testFormatMoroccanPhoneNumber() {
        assertThat(templateService.formatMoroccanPhoneNumber("0612345678")).isEqualTo("+212612345678");
        assertThat(templateService.formatMoroccanPhoneNumber("0711223344")).isEqualTo("+212711223344");
        assertThat(templateService.formatMoroccanPhoneNumber("+212612345678")).isEqualTo("+212612345678");
        assertThat(templateService.formatMoroccanPhoneNumber("212612345678")).isEqualTo("+212612345678");
        assertThat(templateService.formatMoroccanPhoneNumber("06 12 34 56 78")).isEqualTo("+212612345678");
    }
}
