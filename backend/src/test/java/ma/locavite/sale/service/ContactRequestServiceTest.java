package ma.locavite.sale.service;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.notification.service.SmsService;
import ma.locavite.sale.domain.ContactRequest;
import ma.locavite.sale.domain.ContactStatus;
import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.SaleStatus;
import ma.locavite.sale.dto.ContactReplyBody;
import ma.locavite.sale.dto.ContactRequestBody;
import ma.locavite.sale.repository.ContactRequestRepository;
import ma.locavite.sale.repository.SaleListingRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContactRequestServiceTest {

    private ContactRequestRepository contactRepository;
    private SaleListingRepository listingRepository;
    private UserRepository userRepository;
    private CurrentUserService currentUser;
    private JavaMailSender mailSender;
    private SmsService smsService;
    private ContactRequestService service;
    private SaleListing listing;
    private User agency;

    @BeforeEach
    void setUp() {
        contactRepository = mock(ContactRequestRepository.class);
        listingRepository = mock(SaleListingRepository.class);
        userRepository = mock(UserRepository.class);
        currentUser = mock(CurrentUserService.class);
        mailSender = mock(JavaMailSender.class);
        smsService = mock(SmsService.class);
        service = new ContactRequestService(
                contactRepository, listingRepository, userRepository, currentUser, mailSender, smsService);

        listing = new SaleListing();
        listing.setId(7L);
        listing.setAgenceId(12L);
        listing.setTitre("Dacia Duster");
        listing.setCode("SL-123456");
        listing.setStatut(SaleStatus.PUBLISHED);
        listing.setDeleted(false);

        agency = new User();
        agency.setId(12L);
        agency.setEmail("agence@example.com");
        agency.setTelephone("+212600000000");
    }

    @Test
    void createsRequestAndNotifiesAgencyByEmailAndSms() {
        when(listingRepository.findById(7L)).thenReturn(Optional.of(listing));
        when(userRepository.findById(12L)).thenReturn(Optional.of(agency));
        when(smsService.sendSms(anyString(), anyString())).thenReturn(true);
        when(contactRepository.save(any(ContactRequest.class))).thenAnswer(invocation -> {
            ContactRequest request = invocation.getArgument(0);
            request.setId(41L);
            return request;
        });

        var response = service.create(7L, new ContactRequestBody(
                "  Karim Idrissi ", " karim@example.com ", "+212600112233", "  Question sur le véhicule  "));

        assertEquals(41L, response.id());
        assertEquals("Karim Idrissi", response.nom());
        assertEquals("karim@example.com", response.email());
        assertEquals("Question sur le véhicule", response.message());
        assertEquals(ContactStatus.NEW, response.statut());
        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(smsService).sendSms(eq(agency.getTelephone()), contains("Dacia Duster"));
    }

    @Test
    void agencyReplyChangesStatusAndStoresReplyTimestamp() {
        ContactRequest request = new ContactRequest();
        request.setId(41L);
        request.setSaleListing(listing);
        request.setStatut(ContactStatus.NEW);
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.getCurrentUserId()).thenReturn(Optional.of(12L));
        when(contactRepository.findByIdAndSaleListingAgenceId(41L, 12L)).thenReturn(Optional.of(request));
        when(contactRepository.save(request)).thenReturn(request);

        var response = service.reply(41L, new ContactReplyBody("Le véhicule est disponible."));

        assertEquals(ContactStatus.REPLIED, response.statut());
        assertEquals("Le véhicule est disponible.", response.replyMessage());
        assertNotNull(response.repliedAt());
        verify(contactRepository).save(request);
    }
}
