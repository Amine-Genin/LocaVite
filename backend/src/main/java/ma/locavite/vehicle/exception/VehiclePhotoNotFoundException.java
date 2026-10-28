package ma.locavite.vehicle.exception;

public class VehiclePhotoNotFoundException extends RuntimeException {
    public VehiclePhotoNotFoundException(Long photoId) {
        super("Photo non trouvée avec l'id : " + photoId);
    }
}
