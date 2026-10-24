package ma.locavite.notification.service;

import lombok.extern.slf4j.Slf4j;
import ma.locavite.notification.domain.Notification;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    public void envoyerNotification(Notification notification) {
        log.info("Envoi notification [{}] à l'utilisateur {} : {}",
                notification.getType(), notification.getUserId(), notification.getTitre());
    }
}
