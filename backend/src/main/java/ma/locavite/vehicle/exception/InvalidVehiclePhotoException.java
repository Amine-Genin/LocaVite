package ma.locavite.vehicle.exception;

public class InvalidVehiclePhotoException extends RuntimeException {
    public InvalidVehiclePhotoException(String message) {
        super(message);
    }

    public InvalidVehiclePhotoException(String message, Throwable cause) {
        super(message, cause);
    }
}
