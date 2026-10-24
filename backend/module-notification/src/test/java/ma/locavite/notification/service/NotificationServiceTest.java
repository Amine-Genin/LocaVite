package ma.locavite.notification.service;

import ma.locavite.notification.domain.Notification;
import ma.locavite.notification.domain.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceTest {

    private final NotificationService notificationService = new NotificationService();

    @Test
    @DisplayName("Devrait envoyer une notification sans lever d'exception")
    void shouldSendNotificationWithoutException() {
        Notification notif = Notification.builder()
                .userId(1L)
                .titre("Confirmation de réservation")
                .message("Votre réservation a été confirmée avec succès.")
                .type(NotificationType.IN_APP)
                .build();

        assertDoesNotThrow(() -> notificationService.envoyerNotification(notif));
    }
}
