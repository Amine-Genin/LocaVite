package ma.locavite.sale.service;

import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.SaleStatus;
import ma.locavite.sale.domain.VehicleHistory;
import ma.locavite.sale.dto.CreateVehicleHistoryRequest;
import ma.locavite.sale.dto.VehicleHistoryResponse;
import ma.locavite.sale.exception.SaleListingNotFoundException;
import ma.locavite.sale.repository.SaleListingRepository;
import ma.locavite.sale.repository.VehicleHistoryRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class VehicleHistoryService {

    private final SaleListingRepository saleListingRepository;
    private final VehicleHistoryRepository historyRepository;
    private final VehicleHistoryDocumentStorageService documentStorage;
    private final CurrentUserService currentUser;

    public VehicleHistoryService(
            SaleListingRepository saleListingRepository,
            VehicleHistoryRepository historyRepository,
            VehicleHistoryDocumentStorageService documentStorage,
            CurrentUserService currentUser) {
        this.saleListingRepository = saleListingRepository;
        this.historyRepository = historyRepository;
        this.documentStorage = documentStorage;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<VehicleHistoryResponse> getHistory(Long saleListingId) {
        SaleListing listing = findVisibleListing(saleListingId);
        if (listing.getStatut() != SaleStatus.PUBLISHED && !canManage(listing)) {
            throw new SaleListingNotFoundException(saleListingId);
        }

        return historyRepository.findBySaleListingIdOrderByDateAscIdAsc(saleListingId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VehicleHistoryResponse addEvent(
            Long saleListingId,
            CreateVehicleHistoryRequest request,
            MultipartFile document) {
        SaleListing listing = findVisibleListing(saleListingId);
        if (!canManage(listing)) {
            throw new AccessDeniedException("Vous n'êtes pas propriétaire de cette annonce de vente");
        }

        String documentKey = document == null
                ? null
                : documentStorage.upload(saleListingId, document);

        VehicleHistory event = new VehicleHistory();
        event.setSaleListing(listing);
        event.setTypeEvenement(request.typeEvenement());
        event.setDate(request.date());
        event.setDescription(request.description().trim());
        event.setKilometrage(request.kilometrage());
        event.setDocumentUrl(documentKey);
        event.setVerified(false);

        try {
            VehicleHistory saved = historyRepository.save(event);
            return toResponse(saved);
        } catch (RuntimeException e) {
            if (documentKey != null) {
                try {
                    documentStorage.delete(documentKey);
                } catch (RuntimeException cleanupFailure) {
                    e.addSuppressed(cleanupFailure);
                }
            }
            throw e;
        }
    }

    private SaleListing findVisibleListing(Long saleListingId) {
        SaleListing listing = saleListingRepository.findById(saleListingId)
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .orElseThrow(() -> new SaleListingNotFoundException(saleListingId));
        if (listing.getStatut() == SaleStatus.ARCHIVED) {
            throw new SaleListingNotFoundException(saleListingId);
        }
        return listing;
    }

    private boolean canManage(SaleListing listing) {
        return currentUser.isAdmin()
                || currentUser.getCurrentUserId().filter(listing.getAgenceId()::equals).isPresent();
    }

    private VehicleHistoryResponse toResponse(VehicleHistory event) {
        SaleListing listing = event.getSaleListing();
        return new VehicleHistoryResponse(
                event.getId(),
                listing.getId(),
                listing.getVehicle().getId(),
                event.getTypeEvenement(),
                event.getDate(),
                event.getDescription(),
                event.getKilometrage(),
                documentStorage.getPresignedUrl(event.getDocumentUrl()),
                event.isVerified()
        );
    }
}
