package ma.locavite.reservation.service;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.document.domain.DocumentStatus;
import ma.locavite.document.domain.DocumentType;
import ma.locavite.document.repository.DocumentRepository;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.CautionStatus;
import ma.locavite.reservation.domain.ContractLanguage;
import ma.locavite.reservation.domain.ReservationContract;
import ma.locavite.reservation.domain.ReservationExtension;
import ma.locavite.reservation.domain.ReservationOption;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.dto.ExtendReservationRequest;
import ma.locavite.reservation.dto.ReservationExtensionResponse;
import ma.locavite.reservation.dto.ReservationFilter;
import ma.locavite.reservation.dto.ReservationRequest;
import ma.locavite.reservation.dto.ReservationResponse;
import ma.locavite.reservation.dto.QuoteRequest;
import ma.locavite.reservation.dto.PriceQuoteResponse;
import ma.locavite.reservation.exception.ClientDocumentsNotValidatedException;
import ma.locavite.reservation.exception.InvalidReservationDatesException;
import ma.locavite.reservation.exception.InvalidReservationStatusException;
import ma.locavite.reservation.exception.ReservationNotFoundException;
import ma.locavite.reservation.exception.VehicleNotAvailableException;
import ma.locavite.reservation.event.ReservationConfirmedEvent;
import ma.locavite.reservation.event.ReservationExtendedEvent;
import ma.locavite.reservation.mapper.ReservationMapper;
import ma.locavite.reservation.repository.ReservationExtensionRepository;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.reservation.repository.ReservationSpecifications;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.domain.VehicleStatus;
import ma.locavite.vehicle.event.ReservationCreatedEvent;
import ma.locavite.vehicle.exception.VehicleNotFoundException;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.AvailabilityService;
import ma.locavite.vehicle.service.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ReservationService {

    private static final String CODE_PREFIX = "LV-";
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final List<ReservationStatus> BLOCKING_STATUSES = List.of(
            ReservationStatus.PENDING,
            ReservationStatus.CONFIRMED,
            ReservationStatus.ACTIVE
    );

    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ReservationMapper reservationMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AvailabilityService availabilityService;
    private final PricingService pricingService;
    private final ContractService contractService;
    private final ReservationExtensionRepository reservationExtensionRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            VehicleRepository vehicleRepository,
            DocumentRepository documentRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            ReservationMapper reservationMapper,
            ApplicationEventPublisher eventPublisher,
            AvailabilityService availabilityService,
            PricingService pricingService,
            ContractService contractService) {
        this(
                reservationRepository,
                vehicleRepository,
                documentRepository,
                userRepository,
                currentUserService,
                reservationMapper,
                eventPublisher,
                availabilityService,
                pricingService,
                contractService,
                null
        );
    }

    @Autowired
    public ReservationService(
            ReservationRepository reservationRepository,
            VehicleRepository vehicleRepository,
            DocumentRepository documentRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            ReservationMapper reservationMapper,
            ApplicationEventPublisher eventPublisher,
            AvailabilityService availabilityService,
            PricingService pricingService,
            ContractService contractService,
            ReservationExtensionRepository reservationExtensionRepository) {
        this.reservationRepository = reservationRepository;
        this.vehicleRepository = vehicleRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.reservationMapper = reservationMapper;
        this.eventPublisher = eventPublisher;
        this.availabilityService = availabilityService;
        this.pricingService = pricingService;
        this.contractService = contractService;
        this.reservationExtensionRepository = reservationExtensionRepository;
    }

    @Transactional(readOnly = true)
    public PriceQuoteResponse quote(QuoteRequest request) {
        validateDates(request.dateDebut(), request.dateFin());
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(request.vehicleId()));
        return pricingService.calculate(vehicle, request.dateDebut(), request.dateFin(), request.options());
    }

    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        Long currentUserId = getCurrentUserId();

        // 1. Vérification documents client validés (permis + CIN)
        verifyClientDocuments(currentUserId);

        // 2. Validation dates : dateDebut < dateFin, dateDebut >= aujourd'hui
        validateDates(request.dateDebut(), request.dateFin());

        // 3. Vérification existence et disponibilité véhicule (verrou pessimiste pour concurrence)
        Vehicle vehicle = vehicleRepository.findByIdWithLock(request.vehicleId())
                .or(() -> vehicleRepository.findById(request.vehicleId()))
                .orElseThrow(() -> new VehicleNotFoundException(request.vehicleId()));

        if (vehicle.getStatut() == VehicleStatus.ARCHIVED) {
            throw new VehicleNotAvailableException("Ce véhicule n'est plus disponible à la réservation");
        }

        // 4. Vérification chevauchement de réservations
        checkVehicleAvailability(vehicle.getId(), request.dateDebut(), request.dateFin(), null);

        // 5. Calcul du prix et de la caution avec les mêmes règles que le devis
        PriceQuoteResponse quote = pricingService.calculate(
                vehicle, request.dateDebut(), request.dateFin(), request.options());

        // 6. Génération d'un code de réservation unique (format LV-XXXXXX)
        String code = generateUniqueCode();

        // 7. Persistance de la réservation en statut PENDING
        Reservation reservation = new Reservation();
        reservation.setCode(code);
        reservation.setUserId(currentUserId);
        reservation.setVehicleId(vehicle.getId());
        reservation.setDateDebut(request.dateDebut());
        reservation.setDateFin(request.dateFin());
        reservation.setLieuPriseEnCharge(request.lieuPriseEnCharge());
        reservation.setLieuRetour(request.lieuRetour());
        reservation.setPrixTotal(quote.total());
        reservation.setCaution(quote.caution());
        reservation.setPrixSousTotal(quote.sousTotal());
        reservation.setPrixOptions(quote.options());
        reservation.setPrixRemise(quote.remise());
        reservation.setPrixTva(quote.tva());
        reservation.setStatut(ReservationStatus.PENDING);
        reservation.setOptions(request.options() != null ? new HashSet<>(request.options()) : new HashSet<>());

        Reservation saved = reservationRepository.save(reservation);

        // 8. Émission de l'événement ReservationCreatedEvent
        eventPublisher.publishEvent(new ReservationCreatedEvent(
                saved.getId(),
                saved.getVehicleId(),
                saved.getDateDebut(),
                saved.getDateFin()
        ));

        return reservationMapper.toResponse(saved);
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationRequest request) {
        Long currentUserId = getCurrentUserId();

        Reservation reservation = reservationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        // Seul le propriétaire peut modifier sa réservation
        if (!reservation.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Accès refusé : vous n'êtes pas le propriétaire de cette réservation");
        }

        // Modification autorisée uniquement si statut PENDING
        if (reservation.getStatut() != ReservationStatus.PENDING) {
            throw new InvalidReservationStatusException("Seules les réservations en attente (PENDING) peuvent être modifiées");
        }
        if (reservation.getCautionStatut() != CautionStatus.PENDING) {
            throw new InvalidReservationStatusException(
                    "Une réservation dont la caution a été traitée ne peut plus être modifiée");
        }

        // Validation des nouvelles dates
        validateDates(request.dateDebut(), request.dateFin());

        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(request.vehicleId()));

        if (vehicle.getStatut() == VehicleStatus.ARCHIVED) {
            throw new VehicleNotAvailableException("Ce véhicule n'est plus disponible");
        }

        // Vérification de la disponibilité (en excluant la réservation actuelle)
        checkVehicleAvailability(vehicle.getId(), request.dateDebut(), request.dateFin(), reservation.getId());

        // Recalcul du prix et de la caution avec les mêmes règles que le devis
        PriceQuoteResponse quote = pricingService.calculate(
                vehicle, request.dateDebut(), request.dateFin(), request.options());

        reservation.setVehicleId(vehicle.getId());
        reservation.setDateDebut(request.dateDebut());
        reservation.setDateFin(request.dateFin());
        reservation.setLieuPriseEnCharge(request.lieuPriseEnCharge());
        reservation.setLieuRetour(request.lieuRetour());
        reservation.setPrixTotal(quote.total());
        reservation.setCaution(quote.caution());
        reservation.setPrixSousTotal(quote.sousTotal());
        reservation.setPrixOptions(quote.options());
        reservation.setPrixRemise(quote.remise());
        reservation.setPrixTva(quote.tva());
        reservation.setOptions(request.options() != null ? new HashSet<>(request.options()) : new HashSet<>());

        Reservation updated = reservationRepository.save(reservation);

        eventPublisher.publishEvent(new ReservationCreatedEvent(
                updated.getId(),
                updated.getVehicleId(),
                updated.getDateDebut(),
                updated.getDateFin()
        ));

        return reservationMapper.toResponse(updated);
    }

    @Transactional
    public ReservationResponse cancel(Long id) {
        Long currentUserId = getCurrentUserId();

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        boolean isAdmin = isAdmin();
        boolean isOwner = reservation.getUserId().equals(currentUserId);
        boolean isAgence = false;

        if (!isAdmin && !isOwner) {
            Vehicle vehicle = vehicleRepository.findById(reservation.getVehicleId()).orElse(null);
            if (vehicle != null && vehicle.getAgenceId().equals(currentUserId)) {
                isAgence = true;
            }
        }

        if (!isAdmin && !isOwner && !isAgence) {
            throw new AccessDeniedException("Accès refusé : vous ne pouvez pas annuler cette réservation");
        }

        if (reservation.getStatut() == ReservationStatus.CANCELLED) {
            throw new InvalidReservationStatusException("La réservation est déjà annulée");
        }
        if (reservation.getStatut() == ReservationStatus.COMPLETED) {
            throw new InvalidReservationStatusException("Une réservation terminée ne peut pas être annulée");
        }

        reservation.setStatut(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepository.save(reservation);

        availabilityService.invalidateCache();

        return reservationMapper.toResponse(saved);
    }

    @Transactional
    public ReservationResponse confirm(Long id) {
        Long currentUserId = getCurrentUserId();

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        verifyAgencyOrAdminAccess(reservation, currentUserId);

        if (reservation.getStatut() != ReservationStatus.PENDING) {
            throw new InvalidReservationStatusException("Seule une réservation en attente (PENDING) peut être confirmée");
        }

        reservation.setStatut(ReservationStatus.CONFIRMED);
        Reservation saved = reservationRepository.save(reservation);

        ma.locavite.reservation.domain.ReservationContract contract = contractService.generate(
                saved, ma.locavite.reservation.domain.ContractLanguage.FR);
        eventPublisher.publishEvent(new ReservationConfirmedEvent(saved, contract));

        availabilityService.invalidateCache();

        return reservationMapper.toResponse(saved);
    }

    @Transactional
    public ReservationResponse complete(Long id) {
        Long currentUserId = getCurrentUserId();

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        verifyAgencyOrAdminAccess(reservation, currentUserId);

        if (reservation.getStatut() != ReservationStatus.CONFIRMED && reservation.getStatut() != ReservationStatus.ACTIVE) {
            throw new InvalidReservationStatusException("Seule une réservation confirmée ou active peut être marquée terminée");
        }

        reservation.setStatut(ReservationStatus.COMPLETED);
        Reservation saved = reservationRepository.save(reservation);

        availabilityService.invalidateCache();

        return reservationMapper.toResponse(saved);
    }

    @Transactional
    public ReservationResponse extend(Long id, ExtendReservationRequest request) {
        Long currentUserId = getCurrentUserId();

        Reservation reservation = reservationRepository.findByIdForUpdate(id)
                .or(() -> reservationRepository.findById(id))
                .orElseThrow(() -> new ReservationNotFoundException(id));

        if (!reservation.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Accès refusé : vous n'êtes pas le propriétaire de cette réservation");
        }

        if (reservation.getStatut() != ReservationStatus.CONFIRMED
                && reservation.getStatut() != ReservationStatus.ACTIVE) {
            throw new InvalidReservationStatusException(
                    "Seules les réservations confirmées (CONFIRMED) ou actives (ACTIVE) peuvent être prolongées");
        }

        if (request == null || request.newDateFin() == null) {
            throw new InvalidReservationDatesException("La nouvelle date de fin est obligatoire");
        }

        LocalDate ancienneDateFin = reservation.getDateFin();
        LocalDate nouvelleDateFin = request.newDateFin();

        if (!nouvelleDateFin.isAfter(ancienneDateFin)) {
            throw new InvalidReservationDatesException(
                    "La nouvelle date de fin doit être strictement postérieure à la date de fin actuelle");
        }
        if (nouvelleDateFin.isBefore(LocalDate.now())) {
            throw new InvalidReservationDatesException(
                    "La nouvelle date de fin ne peut pas être dans le passé");
        }
        if (!Boolean.TRUE.equals(request.acceptNewPrice())) {
            throw new IllegalArgumentException(
                    "Vous devez accepter le nouveau prix pour prolonger la réservation");
        }

        Vehicle vehicle = vehicleRepository.findByIdWithLock(reservation.getVehicleId())
                .or(() -> vehicleRepository.findById(reservation.getVehicleId()))
                .orElseThrow(() -> new VehicleNotFoundException(reservation.getVehicleId()));

        if (vehicle.getStatut() == VehicleStatus.ARCHIVED) {
            throw new VehicleNotAvailableException("Ce véhicule n'est plus disponible");
        }

        // Vérification de disponibilité sur la période supplémentaire [ancienneDateFin, nouvelleDateFin[
        checkVehicleAvailability(vehicle.getId(), ancienneDateFin, nouvelleDateFin, reservation.getId());

        // Recalcul du prix avec les nouvelles dates [dateDebut, nouvelleDateFin[
        PriceQuoteResponse newQuote = pricingService.calculate(
                vehicle, reservation.getDateDebut(), nouvelleDateFin, reservation.getOptions());

        BigDecimal ancienPrixTotal = reservation.getPrixTotal() != null
                ? reservation.getPrixTotal()
                : pricingService.calculate(
                        vehicle, reservation.getDateDebut(), ancienneDateFin, reservation.getOptions()).total();
        BigDecimal prixSupplementaire = newQuote.total().subtract(ancienPrixTotal);
        if (prixSupplementaire.compareTo(BigDecimal.ZERO) < 0) {
            prixSupplementaire = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        reservation.setDateFin(nouvelleDateFin);
        reservation.setPrixTotal(newQuote.total());
        reservation.setCaution(newQuote.caution());
        reservation.setPrixSousTotal(newQuote.sousTotal());
        reservation.setPrixOptions(newQuote.options());
        reservation.setPrixRemise(newQuote.remise());
        reservation.setPrixTva(newQuote.tva());

        Reservation saved = reservationRepository.save(reservation);

        ReservationExtension extension = new ReservationExtension();
        extension.setReservationId(saved.getId());
        extension.setAncienneDateFin(ancienneDateFin);
        extension.setNouvelleDateFin(nouvelleDateFin);
        extension.setPrixSupplementaire(prixSupplementaire);
        ReservationExtension savedExtension = reservationExtensionRepository != null
                ? reservationExtensionRepository.save(extension)
                : extension;

        ReservationContract updatedContract = contractService.regenerate(saved, ContractLanguage.FR);

        availabilityService.invalidateCache();
        eventPublisher.publishEvent(new ReservationExtendedEvent(saved, savedExtension, updatedContract));

        List<ReservationExtensionResponse> extensions = loadExtensions(saved.getId(), savedExtension);
        return attachExtensions(reservationMapper.toResponse(saved), extensions);
    }

    @Transactional(readOnly = true)
    public List<ReservationExtensionResponse> getExtensions(Long id) {
        Long currentUserId = getCurrentUserId();

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        if (!isAdmin() && !reservation.getUserId().equals(currentUserId)) {
            Vehicle vehicle = vehicleRepository.findById(reservation.getVehicleId()).orElse(null);
            if (vehicle == null || !vehicle.getAgenceId().equals(currentUserId)) {
                throw new AccessDeniedException("Accès refusé : vous ne pouvez pas consulter cette réservation");
            }
        }

        return loadExtensions(id, null);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id) {
        Long currentUserId = getCurrentUserId();

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        if (isAdmin() || reservation.getUserId().equals(currentUserId)) {
            return attachExtensions(
                    reservationMapper.toResponse(reservation),
                    loadExtensions(reservation.getId(), null));
        }

        Vehicle vehicle = vehicleRepository.findById(reservation.getVehicleId()).orElse(null);
        if (vehicle != null && vehicle.getAgenceId().equals(currentUserId)) {
            return attachExtensions(
                    reservationMapper.toResponse(reservation),
                    loadExtensions(reservation.getId(), null));
        }

        throw new AccessDeniedException("Accès refusé : vous ne pouvez pas consulter cette réservation");
    }

    private static ReservationResponse attachExtensions(
            ReservationResponse response,
            List<ReservationExtensionResponse> extensions) {
        return new ReservationResponse(
                response.id(),
                response.code(),
                response.userId(),
                response.vehicleId(),
                response.dateDebut(),
                response.dateFin(),
                response.lieuPriseEnCharge(),
                response.lieuRetour(),
                response.prixTotal(),
                response.caution(),
                response.cautionStatut(),
                response.statut(),
                response.options(),
                extensions != null ? List.copyOf(extensions) : List.of(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    private List<ReservationExtensionResponse> loadExtensions(Long reservationId, ReservationExtension latestExtension) {
        if (reservationExtensionRepository != null && reservationId != null) {
            List<ReservationExtension> stored =
                    reservationExtensionRepository.findByReservationIdOrderByCreatedAtAsc(reservationId);
            if (stored != null && !stored.isEmpty()) {
                return stored.stream().map(ReservationService::toExtensionResponse).toList();
            }
        }
        if (latestExtension != null) {
            return List.of(toExtensionResponse(latestExtension));
        }
        return List.of();
    }

    private static ReservationExtensionResponse toExtensionResponse(ReservationExtension extension) {
        return new ReservationExtensionResponse(
                extension.getId(),
                extension.getReservationId(),
                extension.getAncienneDateFin(),
                extension.getNouvelleDateFin(),
                extension.getPrixSupplementaire(),
                extension.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getMe(Pageable pageable) {
        Long currentUserId = getCurrentUserId();
        return reservationRepository.findByUserId(currentUserId, pageable)
                .map(reservationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getAll(ReservationFilter filter, Pageable pageable) {
        Long currentUserId = getCurrentUserId();

        if (isAdmin()) {
            Specification<Reservation> spec = ReservationSpecifications.withFilter(filter, null, null);
            return reservationRepository.findAll(spec, pageable).map(reservationMapper::toResponse);
        }

        if (isAgence()) {
            List<Long> agencyVehicleIds = vehicleRepository.findIdsByAgenceId(currentUserId);
            Specification<Reservation> spec = ReservationSpecifications.withFilter(filter, agencyVehicleIds, null);
            return reservationRepository.findAll(spec, pageable).map(reservationMapper::toResponse);
        }

        throw new AccessDeniedException("Accès réservé aux agences et administrateurs");
    }

    private void verifyAgencyOrAdminAccess(Reservation reservation, Long currentUserId) {
        if (isAdmin()) {
            return;
        }
        Vehicle vehicle = vehicleRepository.findById(reservation.getVehicleId()).orElse(null);
        if (vehicle != null && vehicle.getAgenceId().equals(currentUserId)) {
            return;
        }
        throw new AccessDeniedException("Accès refusé : permissions insuffisantes pour gérer cette réservation");
    }

    private void verifyClientDocuments(Long userId) {
        boolean hasPermis = documentRepository.existsByUserIdAndTypeAndStatus(
                userId, DocumentType.PERMIS, DocumentStatus.VALIDATED);
        boolean hasCin = documentRepository.existsByUserIdAndTypeAndStatus(
                userId, DocumentType.CIN, DocumentStatus.VALIDATED);

        if (!hasPermis || !hasCin) {
            throw new ClientDocumentsNotValidatedException(
                    "Le client doit avoir son permis et sa CIN validés pour créer une réservation");
        }
    }

    private void validateDates(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null) {
            throw new InvalidReservationDatesException("Les dates de début et de fin sont obligatoires");
        }
        if (!dateDebut.isBefore(dateFin)) {
            throw new InvalidReservationDatesException("La date de début doit être strictement antérieure à la date de fin");
        }
        if (dateDebut.isBefore(LocalDate.now())) {
            throw new InvalidReservationDatesException("La date de début doit être supérieure ou égale à la date du jour");
        }
    }

    /**
     * Vérifie la disponibilité d'un véhicule sur une période donnée [dateDebut, dateFin[.
     *
     * @param vehicleId identifiant du véhicule
     * @param dateDebut date de début souhaitée
     * @param dateFin   date de fin souhaitée
     * @return true si le véhicule est disponible, false sinon
     */
    @Transactional(readOnly = true)
    public boolean isAvailable(Long vehicleId, LocalDate dateDebut, LocalDate dateFin) {
        if (vehicleId == null || dateDebut == null || dateFin == null) {
            return false;
        }
        if (!dateDebut.isBefore(dateFin)) {
            return false;
        }
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            return false;
        }
        Vehicle vehicle = vehicleOpt.get();
        if (vehicle.getStatut() == VehicleStatus.ARCHIVED || vehicle.getStatut() == VehicleStatus.SOLD) {
            return false;
        }

        long count = reservationRepository.countOverlapping(vehicleId, dateDebut, dateFin, null);
        if (count > 0) {
            return false;
        }

        List<Reservation> conflicts = reservationRepository.findOverlappingReservations(
                vehicleId,
                dateDebut,
                dateFin,
                BLOCKING_STATUSES,
                null
        );
        return conflicts.isEmpty();
    }

    private void checkVehicleAvailability(Long vehicleId, LocalDate dateDebut, LocalDate dateFin, Long excludedId) {
        long count = reservationRepository.countOverlapping(vehicleId, dateDebut, dateFin, excludedId);
        if (count > 0) {
            throw new VehicleNotAvailableException("Le véhicule est déjà réservé sur la période sélectionnée");
        }
        List<Reservation> conflicts = reservationRepository.findOverlappingReservations(
                vehicleId,
                dateDebut,
                dateFin,
                BLOCKING_STATUSES,
                excludedId
        );
        if (!conflicts.isEmpty()) {
            throw new VehicleNotAvailableException("Le véhicule est déjà réservé sur la période sélectionnée");
        }
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_PREFIX);
            for (int i = 0; i < 6; i++) {
                sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
            }
            code = sb.toString();
        } while (reservationRepository.existsByCode(code));
        return code;
    }

    public Long getCurrentUserId() {
        if (currentUserService != null && currentUserService.getCurrentUserId().isPresent()) {
            return currentUserService.getCurrentUserId().get();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (auth.getPrincipal() instanceof User user) {
                return user.getId();
            }
            String name = auth.getName();
            if (name != null) {
                Optional<User> byEmail = userRepository.findByEmail(name);
                if (byEmail.isPresent()) {
                    return byEmail.get().getId();
                }
                try {
                    return Long.parseLong(name);
                } catch (NumberFormatException ignored) {}
            }
        }
        throw new AccessDeniedException("Utilisateur non authentifié");
    }

    public boolean isAdmin() {
        if (currentUserService != null && currentUserService.isAdmin()) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ADMIN"));
    }

    public boolean isAgence() {
        if (currentUserService != null && currentUserService.isAgence()) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_AGENCE") || a.equals("AGENCE"));
    }
}
