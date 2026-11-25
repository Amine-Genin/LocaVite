package ma.locavite.sale.listener;

import ma.locavite.sale.event.*;
import ma.locavite.sale.service.SaleNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SaleNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(SaleNotificationListener.class);

    private final SaleNotificationService notificationService;

    public SaleNotificationListener(SaleNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async
    @EventListener
    public void onContactRequestCreated(ContactRequestCreatedEvent event) {
        if (event == null || event.contactRequest() == null || event.listing() == null) return;
        try {
            notificationService.notifyNewContactRequest(event.contactRequest(), event.listing());
        } catch (Exception e) {
            log.error("Erreur lors de la notification pour nouvelle demande de contact [requestId={}] : {}",
                    event.contactRequest().getId(), e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onVisitRequestCreated(VisitRequestCreatedEvent event) {
        if (event == null || event.visitRequest() == null || event.listing() == null) return;
        try {
            notificationService.notifyNewVisitRequest(event.visitRequest(), event.listing());
        } catch (Exception e) {
            log.error("Erreur lors de la notification pour nouvelle demande de visite [visitId={}] : {}",
                    event.visitRequest().getId(), e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onVisitConfirmed(VisitConfirmedEvent event) {
        if (event == null || event.visitRequest() == null || event.listing() == null) return;
        try {
            notificationService.notifyVisitConfirmed(event.visitRequest(), event.listing());
        } catch (Exception e) {
            log.error("Erreur lors de la notification pour visite confirmée [visitId={}] : {}",
                    event.visitRequest().getId(), e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onVisitReminderJ1(VisitReminderJ1Event event) {
        if (event == null || event.visitRequest() == null || event.listing() == null) return;
        try {
            notificationService.notifyVisitReminderJ1(event.visitRequest(), event.listing());
        } catch (Exception e) {
            log.error("Erreur lors de la notification rappel visite J-1 [visitId={}] : {}",
                    event.visitRequest().getId(), e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onListingPublished(SaleListingPublishedEvent event) {
        if (event == null || event.listing() == null) return;
        try {
            notificationService.notifyListingPublished(event.listing());
        } catch (Exception e) {
            log.error("Erreur lors de la notification pour annonce publiée [listingId={}] : {}",
                    event.listing().getId(), e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onListingSold(SaleListingSoldEvent event) {
        if (event == null || event.listing() == null) return;
        try {
            notificationService.notifyListingSold(event.listing());
        } catch (Exception e) {
            log.error("Erreur lors de la notification pour annonce vendue [listingId={}] : {}",
                    event.listing().getId(), e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onContactReplySent(ContactReplySentEvent event) {
        if (event == null || event.contactRequest() == null || event.listing() == null) return;
        try {
            notificationService.notifyMessageReceived(event.contactRequest(), event.listing(), event.replyMessage());
        } catch (Exception e) {
            log.error("Erreur lors de la notification pour message envoyé [requestId={}] : {}",
                    event.contactRequest().getId(), e.getMessage());
        }
    }
}
