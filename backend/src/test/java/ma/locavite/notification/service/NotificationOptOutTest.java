package ma.locavite.notification.service;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.notification.domain.NotificationLog;
import ma.locavite.notification.domain.NotificationPreference;
import ma.locavite.notification.domain.NotificationType;
import ma.locavite.notification.fixtures.NotificationTestFixtures;
import ma.locavite.notification.repository.NotificationLogRepository;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.repository.ReservationContractRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationOptOutTest {

    @Mock
    private SmsService smsService;

    @Mock
    private WhatsAppService whatsAppService;

    @Mock
    private NotificationLogRepository logRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ReservationContractRepository contractRepository;

    private NotificationTemplateService templateService;
    private NotificationService notificationService;

    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        templateService = new NotificationTemplateService();
        notificationService = new NotificationService(
                smsService,
                whatsAppService,
                templateService,
                logRepository,
                userRepository,
                vehicleRepository,
                contractRepository
        );
        vehicle = NotificationTestFixtures.createVehicle();
        lenient().when(vehicleRepository.findById(any())).thenReturn(Optional.of(vehicle));
    }

    @Test
    @DisplayName("Test opt-out : quand preferenceNotification=NONE, aucun SMS/WhatsApp n'est envoyé et log OPTED_OUT créé")
    void testOptOutPreferenceNoneDoesNotSend() {
        User optOutUser = NotificationTestFixtures.createClient(10L, "+212611223344", NotificationPreference.NONE, true);
        Reservation reservation = NotificationTestFixtures.createReservation(
                101L, "LV-OPTOUT-1", optOutUser, vehicle, LocalDate.now().plusDays(2), LocalDate.now().plusDays(5), ReservationStatus.CONFIRMED);

        when(userRepository.findById(10L)).thenReturn(Optional.of(optOutUser));

        boolean result = notificationService.sendConfirmation(reservation);

        assertThat(result).isFalse();
        verifyNoInteractions(smsService);
        verifyNoInteractions(whatsAppService);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(captor.capture());
        NotificationLog savedLog = captor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo("OPTED_OUT");
        assertThat(savedLog.getReservationId()).isEqualTo(101L);
        assertThat(savedLog.getUserId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Test opt-out : quand notificationOptIn=false, aucun envoi et log OPTED_OUT créé")
    void testOptOutFlagFalseDoesNotSend() {
        User optOutUser = NotificationTestFixtures.createClient(20L, "+212611223344", NotificationPreference.SMS, false);
        Reservation reservation = NotificationTestFixtures.createReservation(
                102L, "LV-OPTOUT-2", optOutUser, vehicle, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), ReservationStatus.CONFIRMED);

        when(userRepository.findById(20L)).thenReturn(Optional.of(optOutUser));

        boolean result = notificationService.sendReminderJ1(reservation);

        assertThat(result).isFalse();
        verifyNoInteractions(smsService);
        verifyNoInteractions(whatsAppService);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("OPTED_OUT");
    }

    @Test
    @DisplayName("Test opt-in : quand preference=SMS et optIn=true, smsService est sollicité")
    void testOptInSmsSendsSms() {
        User clientSms = NotificationTestFixtures.createClient(30L, "+212611223344", NotificationPreference.SMS, true);
        Reservation reservation = NotificationTestFixtures.createReservation(
                103L, "LV-OPTIN-SMS", clientSms, vehicle, LocalDate.now().plusDays(1), LocalDate.now().plusDays(4), ReservationStatus.CONFIRMED);

        when(userRepository.findById(30L)).thenReturn(Optional.of(clientSms));
        when(smsService.sendSms(eq(103L), eq(30L), eq("+212611223344"), anyString(), eq(NotificationType.CONFIRMATION)))
                .thenReturn(true);

        boolean result = notificationService.sendConfirmation(reservation);

        assertThat(result).isTrue();
        verify(smsService).sendSms(eq(103L), eq(30L), eq("+212611223344"), anyString(), eq(NotificationType.CONFIRMATION));
        verifyNoInteractions(whatsAppService);
    }

    @Test
    @DisplayName("Test opt-in : quand preference=WHATSAPP et optIn=true, whatsAppService est sollicité")
    void testOptInWhatsAppSendsWhatsApp() {
        User clientWa = NotificationTestFixtures.createClient(40L, "+212611223344", NotificationPreference.WHATSAPP, true);
        Reservation reservation = NotificationTestFixtures.createReservation(
                104L, "LV-OPTIN-WA", clientWa, vehicle, LocalDate.now().plusDays(1), LocalDate.now().plusDays(4), ReservationStatus.CONFIRMED);

        when(userRepository.findById(40L)).thenReturn(Optional.of(clientWa));
        when(whatsAppService.sendWhatsApp(eq(104L), eq(40L), eq("+212611223344"), anyString(), eq(NotificationType.CONFIRMATION)))
                .thenReturn(true);

        boolean result = notificationService.sendConfirmation(reservation);

        assertThat(result).isTrue();
        verify(whatsAppService).sendWhatsApp(eq(104L), eq(40L), eq("+212611223344"), anyString(), eq(NotificationType.CONFIRMATION));
        verifyNoInteractions(smsService);
    }
}
