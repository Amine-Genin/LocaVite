package ma.locavite.sale.service;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.notification.service.SmsService;
import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.SaleStatus;
import ma.locavite.sale.domain.VisitRequest;
import ma.locavite.sale.domain.VisitStatus;
import ma.locavite.sale.domain.VisitType;
import ma.locavite.sale.dto.VisitConfirmBody;
import ma.locavite.sale.dto.VisitRequestBody;
import ma.locavite.sale.dto.VisitRescheduleBody;
import ma.locavite.sale.repository.SaleListingRepository;
import ma.locavite.sale.repository.VisitRequestRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VisitServiceTest {

    private VisitRequestRepository visitRepository;
    private SaleListingRepository listingRepository;
    private UserRepository userRepository;
    private CurrentUserService currentUser;
    private JavaMailSender mailSender;
    private SmsService smsService;
    private VisitService service;
    private SaleListing listing;

    @BeforeEach
    void setUp() {
        visitRepository = mock(VisitRequestRepository.class);
        listingRepository = mock(SaleListingRepository.class);
        userRepository = mock(UserRepository.class);
        currentUser = mock(CurrentUserService.class);
        mailSender = mock(JavaMailSender.class);
        smsService = mock(SmsService.class);
        service = new VisitService(
                visitRepository, listingRepository, userRepository,
                currentUser, mailSender, smsService);

        listing = new SaleListing();
        listing.setId(7L);
        listing.setAgenceId(12L);
        listing.setTitre("Dacia Duster");
        listing.setStatut(SaleStatus.PUBLISHED);
        listing.setDeleted(false);

        when(currentUser.getCurrentUserId()).thenReturn(Optional.of(20L));
        when(currentUser.isAdmin()).thenReturn(false);
        when(listingRepository.findById(7L)).thenReturn(Optional.of(listing));
        when(visitRepository.save(any(VisitRequest.class))).thenAnswer(invocation -> {
            VisitRequest visit = invocation.getArgument(0);
            visit.setId(41L);
            return visit;
        });
        when(smsService.sendSms(anyString(), anyString())).thenReturn(true);
    }

    @Test
    void createsVisitRequestAsPending() {
        var response = service.create(7L, new VisitRequestBody(
                LocalDate.now().plusDays(2),
                "09:00-11:00",
                "Agence Casablanca",
                VisitType.VISITE_SIMPLE));

        assertEquals(41L, response.id());
        assertEquals(VisitStatus.PENDING, response.statut());
        assertEquals(VisitType.VISITE_SIMPLE, response.typeVisite());
        verify(visitRepository).save(any(VisitRequest.class));
    }

    @Test
    void agencyCanConfirmVisitRequest() {
        VisitRequest visit = createVisit();
        when(currentUser.getCurrentUserId()).thenReturn(Optional.of(12L));
        when(visitRepository.findByIdAndSaleListingAgenceId(41L, 12L)).thenReturn(Optional.of(visit));

        var response = service.confirm(41L, new VisitConfirmBody("Apportez votre permis"));

        assertEquals(VisitStatus.CONFIRMED, response.statut());
        assertEquals("Apportez votre permis", response.notes());
        verify(visitRepository).save(visit);
    }

    @Test
    void agencyCanRescheduleVisitRequest() {
        VisitRequest visit = createVisit();
        when(currentUser.getCurrentUserId()).thenReturn(Optional.of(12L));
        when(visitRepository.findByIdAndSaleListingAgenceId(41L, 12L)).thenReturn(Optional.of(visit));

        LocalDate newDate = LocalDate.now().plusDays(5);
        var response = service.reschedule(41L,
                new VisitRescheduleBody(newDate, "14:00-16:00", "Agence Rabat", "Créneau indisponible"));

        assertEquals(VisitStatus.RESCHEDULED, response.statut());
        assertEquals(newDate, response.dateSouhaitee());
        assertEquals("14:00-16:00", response.creneauHoraire());
    }

    @Test
    void clientCanCancelOwnVisitRequest() {
        VisitRequest visit = createVisit();
        when(visitRepository.findByIdAndClientId(41L, 20L)).thenReturn(Optional.of(visit));

        var response = service.cancelByClient(41L);

        assertEquals(VisitStatus.CANCELLED, response.statut());
        verify(visitRepository).save(visit);
    }

    @Test
    void sendsReminderForConfirmedVisitTomorrowToClientAndAgency() {
        VisitRequest visit = createVisit();
        visit.setStatut(VisitStatus.CONFIRMED);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        visit.setDateSouhaitee(tomorrow);

        User client = new User();
        client.setEmail("client@example.com");
        client.setTelephone("+212600000020");
        User agency = new User();
        agency.setEmail("agency@example.com");
        agency.setTelephone("+212600000012");

        when(visitRepository.findByStatutAndDateSouhaiteeAndReminderSentAtIsNull(
                VisitStatus.CONFIRMED, tomorrow)).thenReturn(List.of(visit));
        when(userRepository.findById(20L)).thenReturn(Optional.of(client));
        when(userRepository.findById(12L)).thenReturn(Optional.of(agency));

        assertEquals(1, service.sendReminders(tomorrow));
        assertNotNull(visit.getReminderSentAt());
        verify(smsService).sendSms(eq("+212600000020"), contains("Rappel"));
        verify(smsService).sendSms(eq("+212600000012"), contains("Rappel"));
        verify(visitRepository).save(visit);
    }

    private VisitRequest createVisit() {
        VisitRequest visit = new VisitRequest();
        visit.setId(41L);
        visit.setSaleListing(listing);
        visit.setClientId(20L);
        visit.setDateSouhaitee(LocalDate.now().plusDays(2));
        visit.setCreneauHoraire("09:00-11:00");
        visit.setLieu("Agence Casablanca");
        visit.setTypeVisite(VisitType.ESSAI_ROUTIER);
        visit.setStatut(VisitStatus.PENDING);
        return visit;
    }
}
