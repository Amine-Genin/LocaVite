package ma.locavite.vehicle.service;

import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.domain.VehicleStatus;
import ma.locavite.vehicle.dto.VehicleFilter;
import ma.locavite.vehicle.dto.VehicleRequest;
import ma.locavite.vehicle.dto.VehicleResponse;
import ma.locavite.vehicle.exception.MatriculeAlreadyExistsException;
import ma.locavite.vehicle.exception.VehicleNotFoundException;
import ma.locavite.vehicle.mapper.VehicleMapper;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.spec.VehicleSpecifications;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository repository;
    private final VehicleMapper mapper;
    private final CurrentUserService currentUser;

    public VehicleService(VehicleRepository repository, VehicleMapper mapper, CurrentUserService currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @Transactional
    public VehicleResponse create(VehicleRequest request) {
        Long agenceId = currentUser.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur non authentifié"));

        String matricule = normalize(request.matricule());
        if (repository.existsByMatriculeIgnoreCase(matricule)) {
            throw new MatriculeAlreadyExistsException(matricule);
        }

        Vehicle vehicle = mapper.toEntity(request);
        vehicle.setMatricule(matricule);
        vehicle.setAgenceId(agenceId);
        vehicle.setStatut(request.statut() != null ? request.statut() : VehicleStatus.DRAFT);

        return mapper.toResponse(saveOrConflict(vehicle, matricule));
    }

    public VehicleResponse getById(Long id) {
        Vehicle vehicle = repository.findById(id).orElseThrow(() -> new VehicleNotFoundException(id));
        if (vehicle.getStatut() == VehicleStatus.ARCHIVED) {
            throw new VehicleNotFoundException(id);
        }
        // Un brouillon n'est visible que par son propriétaire ou un admin
        if (vehicle.getStatut() == VehicleStatus.DRAFT && !canManage(vehicle)) {
            throw new VehicleNotFoundException(id);
        }
        return mapper.toResponse(vehicle);
    }

    public Page<VehicleResponse> search(VehicleFilter filter, Pageable pageable) {
        return repository.findAll(VehicleSpecifications.publicSearch(filter), pageable)
                .map(mapper::toResponse);
    }

    @Transactional
    public VehicleResponse update(Long id, VehicleRequest request) {
        Vehicle vehicle = findManageable(id);

        String matricule = normalize(request.matricule());
        if (!matricule.equalsIgnoreCase(vehicle.getMatricule())
                && repository.existsByMatriculeIgnoreCase(matricule)) {
            throw new MatriculeAlreadyExistsException(matricule);
        }

        mapper.updateEntity(request, vehicle);
        vehicle.setMatricule(matricule);
        if (request.statut() != null) {
            vehicle.setStatut(request.statut());
        }

        return mapper.toResponse(saveOrConflict(vehicle, matricule));
    }

    /** Soft delete : le véhicule passe en ARCHIVED. */
    @Transactional
    public void delete(Long id) {
        Vehicle vehicle = findManageable(id);
        vehicle.setStatut(VehicleStatus.ARCHIVED);
        repository.save(vehicle);
    }

    // ---------- helpers ----------

    private Vehicle findManageable(Long id) {
        Vehicle vehicle = repository.findById(id).orElseThrow(() -> new VehicleNotFoundException(id));
        if (vehicle.getStatut() == VehicleStatus.ARCHIVED) {
            throw new VehicleNotFoundException(id);
        }
        if (!canManage(vehicle)) {
            throw new AccessDeniedException("Vous n'êtes pas propriétaire de ce véhicule");
        }
        return vehicle;
    }

    private boolean canManage(Vehicle vehicle) {
        return currentUser.isAdmin()
                || currentUser.getCurrentUserId().filter(id -> id.equals(vehicle.getAgenceId())).isPresent();
    }

    private Vehicle saveOrConflict(Vehicle vehicle, String matricule) {
        try {
            return repository.saveAndFlush(vehicle);
        } catch (DataIntegrityViolationException e) {
            // Course critique : deux requêtes simultanées avec le même matricule
            throw new MatriculeAlreadyExistsException(matricule);
        }
    }

    private static String normalize(String matricule) {
        return matricule.trim().toUpperCase(Locale.ROOT);
    }
}