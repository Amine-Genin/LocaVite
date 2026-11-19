package ma.locavite.sale.exception;

public class InvalidVehicleHistoryDocumentException extends RuntimeException {
    public InvalidVehicleHistoryDocumentException(String message) {
        super(message);
    }

    public InvalidVehicleHistoryDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
