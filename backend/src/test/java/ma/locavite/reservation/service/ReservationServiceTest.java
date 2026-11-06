package ma.locavite.reservation.service;

import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.RoleType;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.document.domain.DocumentStatus;
import ma.locavite.document.domain.DocumentType;
import ma.locavite.document.repository.DocumentRepository;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationOption;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.dto.ReservationFilter;
import ma.locavite.reservation.dto.ReservationRequest;
import ma.locavite.reservation.dto.ReservationResponse;
import ma.locavite.reservation.exception.ClientDocumentsNotValidatedException;
import ma.locavite.reservation.exception.InvalidReservationDatesException;
import ma.locavite.reservation.exception.InvalidReservationStatusException;
import ma.locavite.reservation.exception.VehicleNotAvailableException;
import ma.locavite.reservation.mapper.ReservationMapper;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.domain.VehicleStatus;
import ma.locavite.vehicle.event.ReservationCreatedEvent;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.AvailabilityService;
import ma.locavite.vehicle.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AvailabilityService availabilityService;

    private final ReservationMapper reservationMapper = Mappers.getMapper(ReservationMapper.class);

    private ReservationService reservationService;

    private static final Long CLIENT_ID = 10L;
    private static final Long OTHER_CLIENT_ID = 99L;
    private static final Long AGENCE_ID = 20L;
    private static final Long VEHICLE_ID = 1L;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
                reservationRepository,
                vehicleRepository,
                documentRepository,
                userRepository,
                currentUserService,
                reservationMapper,
                eventPublisher,
                availabilityService
        );
    }

    private Vehicle createVehicle(Long id, Long agenceId, BigDecimal prixJour) {
        Vehicle v = new Vehicle();
        v.setId(id);
        v.setAgenceId(agenceId);
        v.setPrixJour(prixJour);
        v.setStatut(VehicleStatus.PUBLISHED);
        return v;
    }

    private void mockClientWithValidatedDocs(Long userId) {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(documentRepository.existsByUserIdAndTypeAndStatus(userId, DocumentType.PERMIS, DocumentStatus.VALIDATED))
                .thenReturn(true);
        when(documentRepository.existsByUserIdAndTypeAndStatus(userId, DocumentType.CIN, DocumentStatus.VALIDATED))
                .thenReturn(true);
    }

    @Test
    @DisplayName("Test : création valide → 201 + code généré au format LV-XXXXXX")
    void create_whenValidRequest_savesReservationAndEmitsEvent() {
        mockClientWithValidatedDocs(CLIENT_ID);

        LocalDate debut = LocalDate.now().plusDays(2);
        LocalDate fin = LocalDate.now().plusDays(5); // 3 jours
        Vehicle vehicle = createVehicle(VEHICLE_ID, AGENCE_ID, new BigDecimal("400.00"));

        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(reservationRepository.findOverlappingReservations(eq(VEHICLE_ID), eq(debut), eq(fin), any(), eq(null)))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.existsByCode(anyString())).thenReturn(false);

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        ReservationRequest request = new ReservationRequest(
                VEHICLE_ID,
                debut,
                fin,
                "Aéroport Casablanca",
                "Aéroport Casablanca",
                Set.of(ReservationOption.GPS, ReservationOption.BABY_SEAT)
        );

        ReservationResponse response = reservationService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.code()).isNotNull();
        assertThat(response.code()).matches("^LV-[A-Z0-9]{6}$");
        assertThat(response.statut()).isEqualTo(ReservationStatus.PENDING);
        assertThat(response.userId()).isEqualTo(CLIENT_ID);
        assertThat(response.vehicleId()).isEqualTo(VEHICLE_ID);
        // Prix: 3 jours * 400 = 1200 + GPS (50) + BABY_SEAT (50) = 1300.00
        assertThat(response.prixTotal()).isEqualByComparingTo(new BigDecimal("1300.00"));
        // Caution: max(3000, 400 * 5 = 2000) = 3000.00
        assertThat(response.caution()).isEqualByComparingTo(new BigDecimal("3000.00"));

        verify(eventPublisher).publishEvent(any(ReservationCreatedEvent.class));
    }

    @Test
    @DisplayName("Test : dates invalides (dateDebut >= dateFin) → 400")
    void create_whenDateDebutAfterDateFin_throwsInvalidReservationDatesException() {
        mockClientWithValidatedDocs(CLIENT_ID);

        LocalDate debut = LocalDate.now().plusDays(5);
        LocalDate fin = LocalDate.now().plusDays(2);

        ReservationRequest request = new ReservationRequest(
                VEHICLE_ID, debut, fin, "Casa", "Casa", Collections.emptySet()
        );

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(InvalidReservationDatesException.class)
                .hasMessageContaining("antérieure");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test : dates invalides (dateDebut dans le passé) → 400")
    void create_whenDateDebutInPast_throwsInvalidReservationDatesException() {
        mockClientWithValidatedDocs(CLIENT_ID);

        LocalDate debut = LocalDate.now().minusDays(1);
        LocalDate fin = LocalDate.now().plusDays(2);

        ReservationRequest request = new ReservationRequest(
                VEHICLE_ID, debut, fin, "Casa", "Casa", Collections.emptySet()
        );

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(InvalidReservationDatesException.class)
                .hasMessageContaining("jour");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test : véhicule déjà réservé sur la période → 409 Conflict")
    void create_whenVehicleAlreadyReserved_throwsVehicleNotAvailableException() {
        mockClientWithValidatedDocs(CLIENT_ID);

        LocalDate debut = LocalDate.now().plusDays(2);
        LocalDate fin = LocalDate.now().plusDays(5);
        Vehicle vehicle = createVehicle(VEHICLE_ID, AGENCE_ID, new BigDecimal("400.00"));

        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        Reservation existingReservation = new Reservation();
        existingReservation.setId(50L);
        existingReservation.setDateDebut(debut);
        existingReservation.setDateFin(fin);

        when(reservationRepository.findOverlappingReservations(eq(VEHICLE_ID), eq(debut), eq(fin), any(), eq(null)))
                .thenReturn(List.of(existingReservation));

        ReservationRequest request = new ReservationRequest(
                VEHICLE_ID, debut, fin, "Casa", "Casa", Collections.emptySet()
        );

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(VehicleNotAvailableException.class)
                .hasMessageContaining("déjà réservé");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test : client sans documents validés (aucun document) → 403")
    void create_whenNoValidatedDocuments_throwsClientDocumentsNotValidatedException() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(CLIENT_ID));
        when(documentRepository.existsByUserIdAndTypeAndStatus(CLIENT_ID, DocumentType.PERMIS, DocumentStatus.VALIDATED))
                .thenReturn(false);

        LocalDate debut = LocalDate.now().plusDays(2);
        LocalDate fin = LocalDate.now().plusDays(5);

        ReservationRequest request = new ReservationRequest(
                VEHICLE_ID, debut, fin, "Casa", "Casa", Collections.emptySet()
        );

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(ClientDocumentsNotValidatedException.class)
                .hasMessageContaining("permis et sa CIN validés");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test : client avec uniquement permis validé sans CIN → 403")
    void create_whenOnlyPermisValidatedWithoutCIN_throwsClientDocumentsNotValidatedException() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(CLIENT_ID));
        when(documentRepository.existsByUserIdAndTypeAndStatus(CLIENT_ID, DocumentType.PERMIS, DocumentStatus.VALIDATED))
                .thenReturn(true);
        when(documentRepository.existsByUserIdAndTypeAndStatus(CLIENT_ID, DocumentType.CIN, DocumentStatus.VALIDATED))
                .thenReturn(false);

        LocalDate debut = LocalDate.now().plusDays(2);
        LocalDate fin = LocalDate.now().plusDays(5);

        ReservationRequest request = new ReservationRequest(
                VEHICLE_ID, debut, fin, "Casa", "Casa", Collections.emptySet()
        );

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(ClientDocumentsNotValidatedException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test : client ne peut voir que ses réservations (autre client → 403)")
    void getById_whenOtherClientAccessesReservation_throwsAccessDeniedException() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(OTHER_CLIENT_ID));
        when(currentUserService.isAdmin()).thenReturn(false);

        Reservation res = new Reservation();
        res.setId(100L);
        res.setUserId(CLIENT_ID);
        res.setVehicleId(VEHICLE_ID);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(res));
        Vehicle vehicle = createVehicle(VEHICLE_ID, AGENCE_ID, new BigDecimal("350.00"));
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> reservationService.getById(100L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Accès refusé");
    }

    @Test
    @DisplayName("Test : client peut voir sa propre réservation (200 OK)")
    void getById_whenOwnerAccessesReservation_succeeds() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(CLIENT_ID));
        when(currentUserService.isAdmin()).thenReturn(false);

        Reservation res = new Reservation();
        res.setId(100L);
        res.setCode("LV-ABC123");
        res.setUserId(CLIENT_ID);
        res.setVehicleId(VEHICLE_ID);
        res.setStatut(ReservationStatus.PENDING);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(res));

        ReservationResponse response = reservationService.getById(100L);
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.code()).isEqualTo("LV-ABC123");
    }

    @Test
    @DisplayName("Test : agence voit les réservations de ses véhicules (200 OK)")
    void getById_whenAgencyOwnsVehicle_succeeds() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(AGENCE_ID));
        when(currentUserService.isAdmin()).thenReturn(false);

        Reservation res = new Reservation();
        res.setId(100L);
        res.setCode("LV-ABC123");
        res.setUserId(CLIENT_ID);
        res.setVehicleId(VEHICLE_ID);
        res.setStatut(ReservationStatus.CONFIRMED);

        Vehicle vehicle = createVehicle(VEHICLE_ID, AGENCE_ID, new BigDecimal("350.00"));

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(res));
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        ReservationResponse response = reservationService.getById(100L);
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Test : agence ne peut pas voir les réservations d'un véhicule d'une autre agence (403)")
    void getById_whenAgencyDoesNotOwnVehicle_throwsAccessDeniedException() {
        Long otherAgenceId = 777L;
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(otherAgenceId));
        when(currentUserService.isAdmin()).thenReturn(false);

        Reservation res = new Reservation();
        res.setId(100L);
        res.setUserId(CLIENT_ID);
        res.setVehicleId(VEHICLE_ID);

        Vehicle vehicle = createVehicle(VEHICLE_ID, AGENCE_ID, new BigDecimal("350.00")); // Agence 20 != 777

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(res));
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> reservationService.getById(100L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Test : modification autorisée si propriétaire et statut PENDING")
    void update_whenOwnerAndPending_succeeds() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(CLIENT_ID));

        Reservation res = new Reservation();
        res.setId(100L);
        res.setUserId(CLIENT_ID);
        res.setVehicleId(VEHICLE_ID);
        res.setDateDebut(LocalDate.now().plusDays(2));
        res.setDateFin(LocalDate.now().plusDays(4));
        res.setStatut(ReservationStatus.PENDING);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(res));

        LocalDate newDebut = LocalDate.now().plusDays(3);
        LocalDate newFin = LocalDate.now().plusDays(6);
        Vehicle vehicle = createVehicle(VEHICLE_ID, AGENCE_ID, new BigDecimal("500.00"));

        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(reservationRepository.findOverlappingReservations(eq(VEHICLE_ID), eq(newDebut), eq(newFin), any(), eq(100L)))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationRequest updateReq = new ReservationRequest(
                VEHICLE_ID, newDebut, newFin, "Gare Rabat", "Gare Rabat", Set.of(ReservationOption.GPS)
        );

        ReservationResponse updated = reservationService.update(100L, updateReq);

        assertThat(updated).isNotNull();
        assertThat(updated.lieuPriseEnCharge()).isEqualTo("Gare Rabat");
        // 3 jours * 500 = 1500 + GPS (50) = 1550.00
        assertThat(updated.prixTotal()).isEqualByComparingTo(new BigDecimal("1550.00"));
    }

    @Test
    @DisplayName("Test : modification rejetée si statut non PENDING (CONFIRMED) → 400")
    void update_whenNotPending_throwsInvalidReservationStatusException() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(CLIENT_ID));

        Reservation res = new Reservation();
        res.setId(100L);
        res.setUserId(CLIENT_ID);
        res.setStatut(ReservationStatus.CONFIRMED);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(res));

        ReservationRequest updateReq = new ReservationRequest(
                VEHICLE_ID, LocalDate.now().plusDays(3), LocalDate.now().plusDays(6), "Casa", "Casa", null
        );

        assertThatThrownBy(() -> reservationService.update(100L, updateReq))
                .isInstanceOf(InvalidReservationStatusException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("Test : une agence reconnue par CurrentUserService est bien détectée")
    void isAgence_whenCurrentUserServiceReportsAgency_returnsTrue() {
        SecurityContextHolder.clearContext();
        when(currentUserService.isAgence()).thenReturn(true);

        assertThat(reservationService.isAgence()).isTrue();
    }

    @Test
    @DisplayName("Test : annulation d'une réservation (DELETE) → statut CANCELLED")
    void cancel_whenOwnerCancels_setsStatusCancelled() {
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(CLIENT_ID));
        when(currentUserService.isAdmin()).thenReturn(false);

        Reservation res = new Reservation();
        res.setId(100L);
        res.setUserId(CLIENT_ID);
        res.setStatut(ReservationStatus.PENDING);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(res));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse cancelled = reservationService.cancel(100L);

        assertThat(cancelled.statut()).isEqualTo(ReservationStatus.CANCELLED);
        verify(availabilityService).invalidateCache();
    }
}
