package ma.locavite.sale.service;

import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.SaleStatus;
import ma.locavite.sale.domain.VehicleCondition;
import ma.locavite.sale.dto.SaleListingFilter;
import ma.locavite.sale.dto.SaleListingRequest;
import ma.locavite.sale.dto.SaleListingResponse;
import ma.locavite.sale.exception.SaleListingNotFoundException;
import ma.locavite.sale.exception.VinAlreadyExistsException;
import ma.locavite.sale.mapper.SaleListingMapper;
import ma.locavite.sale.repository.SaleListingRepository;
import ma.locavite.sale.spec.SaleListingSpecifications;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.exception.VehicleNotFoundException;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class SaleListingService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SaleListingRepository repository;
    private final VehicleRepository vehicleRepository;
    private final SaleListingMapper mapper;
    private final CurrentUserService currentUser;

    public SaleListingService(
            SaleListingRepository repository,
            VehicleRepository vehicleRepository,
            SaleListingMapper mapper,
            CurrentUserService currentUser) {
        this.repository = repository;
        this.vehicleRepository = vehicleRepository;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @Transactional
    public SaleListingResponse create(SaleListingRequest request) {
        Long currentUserId = currentUser.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur non authentifié"));

        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(request.vehicleId()));

        if (!canManageVehicle(vehicle, currentUserId)) {
            throw new AccessDeniedException("Vous ne pouvez créer une annonce que pour vos propres véhicules");
        }

        String vin = normalizeVin(request.numeroChassis());
        if (vin != null && repository.existsByNumeroChassisIgnoreCaseAndDeletedFalse(vin)) {
            throw new VinAlreadyExistsException(vin);
        }

        SaleListing listing = mapper.toEntity(request);
        listing.setVehicle(vehicle);
        listing.setAgenceId(vehicle.getAgenceId());
        listing.setCode(generateUniqueCode());
        listing.setNumeroChassis(vin);
        listing.setDeleted(false);

        if (request.prixNego() != null) {
            listing.setPrixNego(request.prixNego());
        } else {
            listing.setPrixNego(false);
        }

        if (request.premierProprietaire() != null) {
            listing.setPremierProprietaire(request.premierProprietaire());
        } else {
            listing.setPremierProprietaire(request.etat() == VehicleCondition.NEW);
        }

        if (request.nombreProprietaires() != null) {
            listing.setNombreProprietaires(request.nombreProprietaires());
        } else {
            listing.setNombreProprietaires(request.etat() == VehicleCondition.NEW ? 0 : 1);
        }

        SaleStatus statut = request.statut() != null ? request.statut() : SaleStatus.DRAFT;
        listing.setStatut(statut);
        if (statut == SaleStatus.PUBLISHED) {
            listing.setPublishedAt(Instant.now());
        } else if (statut == SaleStatus.SOLD) {
            listing.setSoldAt(Instant.now());
        }

        SaleListing saved = repository.save(listing);
        return mapper.toResponse(saved);
    }

    public SaleListingResponse getById(Long id) {
        SaleListing listing = repository.findById(id)
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .orElseThrow(() -> new SaleListingNotFoundException(id));

        if (listing.getStatut() == SaleStatus.ARCHIVED) {
            throw new SaleListingNotFoundException(id);
        }

        if (listing.getStatut() == SaleStatus.DRAFT && !canManage(listing)) {
            throw new SaleListingNotFoundException(id);
        }

        return mapper.toResponse(listing);
    }

    public Page<SaleListingResponse> search(SaleListingFilter filter, Pageable pageable) {
        boolean isOwnerFilteringOwn = filter != null
                && filter.agenceId() != null
                && currentUser.getCurrentUserId().filter(uid -> uid.equals(filter.agenceId())).isPresent();

        boolean onlyPublished = !currentUser.isAdmin() && !isOwnerFilteringOwn;

        return repository.findAll(SaleListingSpecifications.search(filter, onlyPublished), pageable)
                .map(mapper::toResponse);
    }

    @Transactional
    public SaleListingResponse update(Long id, SaleListingRequest request) {
        SaleListing listing = findManageable(id);

        if (request.vehicleId() != null && !request.vehicleId().equals(listing.getVehicle().getId())) {
            Vehicle newVehicle = vehicleRepository.findById(request.vehicleId())
                    .orElseThrow(() -> new VehicleNotFoundException(request.vehicleId()));
            if (!canManageVehicle(newVehicle, listing.getAgenceId())) {
                throw new AccessDeniedException("Le véhicule associé doit appartenir à votre agence");
            }
            listing.setVehicle(newVehicle);
            listing.setAgenceId(newVehicle.getAgenceId());
        }

        String vin = normalizeVin(request.numeroChassis());
        if (vin != null && repository.existsByNumeroChassisIgnoreCaseAndIdNotAndDeletedFalse(vin, id)) {
            throw new VinAlreadyExistsException(vin);
        }

        mapper.updateEntity(request, listing);
        listing.setNumeroChassis(vin);

        if (request.prixNego() != null) {
            listing.setPrixNego(request.prixNego());
        }
        if (request.premierProprietaire() != null) {
            listing.setPremierProprietaire(request.premierProprietaire());
        }
        if (request.nombreProprietaires() != null) {
            listing.setNombreProprietaires(request.nombreProprietaires());
        }

        if (request.statut() != null && request.statut() != listing.getStatut()) {
            applyStatusTransition(listing, request.statut());
        }

        SaleListing saved = repository.save(listing);
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        SaleListing listing = findManageable(id);
        listing.setDeleted(true);
        listing.setStatut(SaleStatus.ARCHIVED);
        repository.save(listing);
    }

    @Transactional
    public SaleListingResponse publish(Long id) {
        SaleListing listing = findManageable(id);
        listing.setStatut(SaleStatus.PUBLISHED);
        if (listing.getPublishedAt() == null) {
            listing.setPublishedAt(Instant.now());
        }
        SaleListing saved = repository.save(listing);
        return mapper.toResponse(saved);
    }

    @Transactional
    public SaleListingResponse markSold(Long id) {
        SaleListing listing = findManageable(id);
        listing.setStatut(SaleStatus.SOLD);
        listing.setSoldAt(Instant.now());
        SaleListing saved = repository.save(listing);
        return mapper.toResponse(saved);
    }

    // ---------- helpers ----------

    private SaleListing findManageable(Long id) {
        SaleListing listing = repository.findById(id)
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .orElseThrow(() -> new SaleListingNotFoundException(id));

        if (!canManage(listing)) {
            throw new AccessDeniedException("Vous n'êtes pas propriétaire de cette annonce de vente");
        }
        return listing;
    }

    private boolean canManage(SaleListing listing) {
        return currentUser.isAdmin()
                || currentUser.getCurrentUserId().filter(uid -> uid.equals(listing.getAgenceId())).isPresent();
    }

    private boolean canManageVehicle(Vehicle vehicle, Long userId) {
        return currentUser.isAdmin()
                || (vehicle.getAgenceId() != null && vehicle.getAgenceId().equals(userId));
    }

    private void applyStatusTransition(SaleListing listing, SaleStatus newStatus) {
        listing.setStatut(newStatus);
        if (newStatus == SaleStatus.PUBLISHED && listing.getPublishedAt() == null) {
            listing.setPublishedAt(Instant.now());
        } else if (newStatus == SaleStatus.SOLD && listing.getSoldAt() == null) {
            listing.setSoldAt(Instant.now());
        }
    }

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            if (++attempts > 20) {
                throw new IllegalStateException("Impossible de générer un code unique pour l'annonce de vente");
            }
            StringBuilder sb = new StringBuilder("SL-");
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (repository.existsByCode(code));
        return code;
    }

    private static String normalizeVin(String vin) {
        if (vin == null || vin.isBlank()) {
            return null;
        }
        return vin.trim().toUpperCase(Locale.ROOT);
    }
}
