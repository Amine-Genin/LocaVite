package ma.locavite.vehicle.service;

import ma.locavite.auth.domain.User;
import ma.locavite.document.exception.FileTooLargeException;
import ma.locavite.vehicle.domain.VehiclePhoto;
import ma.locavite.vehicle.dto.VehiclePhotoResponse;
import ma.locavite.vehicle.exception.InvalidVehiclePhotoException;
import ma.locavite.vehicle.exception.VehicleNotFoundException;
import ma.locavite.vehicle.exception.VehiclePhotoNotFoundException;
import ma.locavite.vehicle.repository.VehiclePhotoRepository;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class VehiclePhotoService {

    public static final int MAX_PHOTOS_PER_VEHICLE = 10;
    public static final long MAX_PHOTO_SIZE = 8L * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Logger log = LoggerFactory.getLogger(VehiclePhotoService.class);

    private final VehiclePhotoRepository photoRepository;
    private final JdbcTemplate jdbcTemplate;
    private final VehiclePhotoStorageService storageService;
    private final ImageCompressionService compressionService;
    private final Tika tika = new Tika();

    public VehiclePhotoService(VehiclePhotoRepository photoRepository, JdbcTemplate jdbcTemplate,
                               VehiclePhotoStorageService storageService,
                               ImageCompressionService compressionService) {
        this.photoRepository = photoRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.storageService = storageService;
        this.compressionService = compressionService;
    }

    @Transactional
    public List<VehiclePhotoResponse> upload(Long vehicleId, User currentUser, List<MultipartFile> files) {
        requireUser(currentUser);
        lockAndCheckVehicleOwner(vehicleId, currentUser, true);

        if (files == null || files.isEmpty()) {
            throw new InvalidVehiclePhotoException("Au moins une photo est requise");
        }
        if (files.size() > MAX_PHOTOS_PER_VEHICLE) {
            throw new InvalidVehiclePhotoException("Vous ne pouvez téléverser que 10 photos au maximum par véhicule");
        }

        List<VehiclePhoto> currentPhotos = photoRepository.findByVehicleIdOrderByOrdreAsc(vehicleId);
        if (currentPhotos.size() + files.size() > MAX_PHOTOS_PER_VEHICLE) {
            throw new InvalidVehiclePhotoException("Un véhicule ne peut pas avoir plus de 10 photos");
        }

        List<PreparedPhoto> preparedPhotos = files.stream().map(file -> prepare(vehicleId, file)).toList();
        int nextOrder = currentPhotos.stream().mapToInt(VehiclePhoto::getOrdre).max().orElse(0) + 1;
        List<String> storedPaths = new ArrayList<>();

        try {
            List<VehiclePhotoResponse> responses = new ArrayList<>();
            for (int index = 0; index < preparedPhotos.size(); index++) {
                PreparedPhoto prepared = preparedPhotos.get(index);
                storageService.store(prepared.path(), prepared.original(), prepared.mimeType());
                storedPaths.add(prepared.path());
                storageService.store(prepared.thumbnailPath(), prepared.thumbnail(), "image/jpeg");
                storedPaths.add(prepared.thumbnailPath());

                VehiclePhoto photo = new VehiclePhoto();
                photo.setVehicleId(vehicleId);
                photo.setPath(prepared.path());
                photo.setThumbnailPath(prepared.thumbnailPath());
                photo.setOrdre(nextOrder + index);
                photo.setPrimary(currentPhotos.isEmpty() && index == 0);
                VehiclePhoto savedPhoto = photoRepository.save(photo);
                responses.add(toResponse(savedPhoto));
            }
            photoRepository.flush();
            return responses;
        } catch (RuntimeException e) {
            cleanupStoredObjects(storedPaths, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<VehiclePhotoResponse> getPhotos(Long vehicleId, User currentUser) {
        requireUser(currentUser);
        lockAndCheckVehicleOwner(vehicleId, currentUser, false);
        return photoRepository.findByVehicleIdOrderByOrdreAsc(vehicleId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long vehicleId, Long photoId, User currentUser) {
        requireUser(currentUser);
        lockAndCheckVehicleOwner(vehicleId, currentUser, true);
        VehiclePhoto photo = findPhoto(vehicleId, photoId);

        List<VehiclePhoto> orderedPhotos = photoRepository.findByVehicleIdOrderByOrdreAsc(vehicleId);
        VehiclePhoto nextPrimary = photo.isPrimary()
                ? orderedPhotos.stream().filter(candidate -> !candidate.getId().equals(photoId)).findFirst().orElse(null)
                : null;

        photoRepository.delete(photo);
        if (nextPrimary != null) {
            photoRepository.flush();
            nextPrimary.setPrimary(true);
            photoRepository.save(nextPrimary);
        }

        storageService.delete(photo.getPath());
        storageService.delete(photo.getThumbnailPath());
    }

    @Transactional
    public VehiclePhotoResponse setPrimary(Long vehicleId, Long photoId, User currentUser) {
        requireUser(currentUser);
        lockAndCheckVehicleOwner(vehicleId, currentUser, true);
        VehiclePhoto selectedPhoto = findPhoto(vehicleId, photoId);
        List<VehiclePhoto> photos = photoRepository.findByVehicleIdOrderByOrdreAsc(vehicleId);
        photos.forEach(photo -> photo.setPrimary(false));
        photoRepository.saveAllAndFlush(photos);
        selectedPhoto.setPrimary(true);
        photoRepository.save(selectedPhoto);
        return toResponse(selectedPhoto);
    }

    private PreparedPhoto prepare(Long vehicleId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidVehiclePhotoException("Les photos ne peuvent pas être vides");
        }
        if (file.getSize() > MAX_PHOTO_SIZE) {
            throw new FileTooLargeException("Chaque photo ne doit pas dépasser 8 Mo");
        }

        try {
            byte[] original = file.getBytes();
            String mimeType = tika.detect(original);
            if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
                throw new InvalidVehiclePhotoException("Type de photo non autorisé. Types acceptés : JPEG, PNG et WebP");
            }
            String extension = switch (mimeType) {
                case "image/jpeg" -> "jpg";
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                default -> throw new InvalidVehiclePhotoException("Type de photo non autorisé");
            };
            String uuid = UUID.randomUUID().toString();
            String path = vehicleId + "/" + uuid + "." + extension;
            byte[] thumbnail = compressionService.createThumbnail(original);
            return new PreparedPhoto(path, vehicleId + "/" + uuid + "_thumbnail.jpg",
                    original, thumbnail, mimeType);
        } catch (IOException e) {
            throw new InvalidVehiclePhotoException("Impossible de lire la photo téléversée", e);
        }
    }

    private void lockAndCheckVehicleOwner(Long vehicleId, User currentUser, boolean lock) {
        if (vehicleId == null) {
            throw new VehicleNotFoundException(null);
        }
        String query = "SELECT agence_id FROM vehicles WHERE id = ?" + (lock ? " FOR UPDATE" : "");
        List<Long> ownerIds = jdbcTemplate.queryForList(query, Long.class, vehicleId);
        if (ownerIds.isEmpty()) {
            throw new VehicleNotFoundException(vehicleId);
        }
        if (!ownerIds.get(0).equals(currentUser.getId())) {
            throw new AccessDeniedException("Accès refusé : ce véhicule ne vous appartient pas");
        }
    }

    private VehiclePhoto findPhoto(Long vehicleId, Long photoId) {
        return photoRepository.findByIdAndVehicleId(photoId, vehicleId)
                .orElseThrow(() -> new VehiclePhotoNotFoundException(photoId));
    }

    private VehiclePhotoResponse toResponse(VehiclePhoto photo) {
        return new VehiclePhotoResponse(
                photo.getId(),
                photo.getVehicleId(),
                storageService.getPresignedUrl(photo.getPath()),
                storageService.getPresignedUrl(photo.getThumbnailPath()),
                photo.isPrimary(),
                photo.getOrdre(),
                photo.getUploadedAt()
        );
    }

    private void cleanupStoredObjects(List<String> storedPaths, RuntimeException originalException) {
        for (String path : storedPaths) {
            try {
                storageService.delete(path);
            } catch (RuntimeException cleanupException) {
                log.error("Impossible de nettoyer l'objet MinIO '{}' après un échec d'upload", path, cleanupException);
                originalException.addSuppressed(cleanupException);
            }
        }
    }

    private void requireUser(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new AccessDeniedException("Authentification requise");
        }
    }

    private record PreparedPhoto(String path, String thumbnailPath, byte[] original, byte[] thumbnail,
                                 String mimeType) {
    }
}
