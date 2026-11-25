package ma.locavite.sale.listener;

import ma.locavite.sale.domain.ContactRequest;
import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.VisitRequest;
import ma.locavite.sale.event.*;
import ma.locavite.sale.service.SaleNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class SaleNotificationListenerTest {

    private SaleNotificationService notificationService;
    private SaleNotificationListener listener;

    @BeforeEach
    void setUp() {
        notificationService = mock(SaleNotificationService.class);
        listener = new SaleNotificationListener(notificationService);
    }

    @Test
    void onContactRequestCreated_dispatchesToService() {
        ContactRequest request = new ContactRequest();
        SaleListing listing = new SaleListing();
        listener.onContactRequestCreated(new ContactRequestCreatedEvent(request, listing));
        verify(notificationService).notifyNewContactRequest(request, listing);
    }

    @Test
    void onVisitRequestCreated_dispatchesToService() {
        VisitRequest visit = new VisitRequest();
        SaleListing listing = new SaleListing();
        listener.onVisitRequestCreated(new VisitRequestCreatedEvent(visit, listing));
        verify(notificationService).notifyNewVisitRequest(visit, listing);
    }

    @Test
    void onVisitConfirmed_dispatchesToService() {
        VisitRequest visit = new VisitRequest();
        SaleListing listing = new SaleListing();
        listener.onVisitConfirmed(new VisitConfirmedEvent(visit, listing));
        verify(notificationService).notifyVisitConfirmed(visit, listing);
    }

    @Test
    void onVisitReminderJ1_dispatchesToService() {
        VisitRequest visit = new VisitRequest();
        SaleListing listing = new SaleListing();
        listener.onVisitReminderJ1(new VisitReminderJ1Event(visit, listing));
        verify(notificationService).notifyVisitReminderJ1(visit, listing);
    }

    @Test
    void onListingPublished_dispatchesToService() {
        SaleListing listing = new SaleListing();
        listener.onListingPublished(new SaleListingPublishedEvent(listing));
        verify(notificationService).notifyListingPublished(listing);
    }

    @Test
    void onListingSold_dispatchesToService() {
        SaleListing listing = new SaleListing();
        listener.onListingSold(new SaleListingSoldEvent(listing));
        verify(notificationService).notifyListingSold(listing);
    }

    @Test
    void onContactReplySent_dispatchesToService() {
        ContactRequest request = new ContactRequest();
        SaleListing listing = new SaleListing();
        listener.onContactReplySent(new ContactReplySentEvent(request, listing, "Hello"));
        verify(notificationService).notifyMessageReceived(request, listing, "Hello");
    }
}
