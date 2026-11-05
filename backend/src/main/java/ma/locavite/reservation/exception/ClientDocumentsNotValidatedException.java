package ma.locavite.reservation.exception;

import org.springframework.security.access.AccessDeniedException;

public class ClientDocumentsNotValidatedException extends AccessDeniedException {
    public ClientDocumentsNotValidatedException(String message) {
        super(message);
    }
}
