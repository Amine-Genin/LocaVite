package ma.locavite.payment.exception;

import ma.locavite.payment.controller.AdminPaymentController;
import ma.locavite.payment.cmi.controller.CmiPaymentController;
import ma.locavite.payment.cmi.exception.CmiIpNotAllowedException;
import ma.locavite.payment.cmi.exception.CmiRateLimitExceededException;
import ma.locavite.payment.cmi.exception.CmiSessionExpiredException;
import ma.locavite.payment.cmi.exception.CmiSignatureException;
import ma.locavite.payment.cmi.exception.CmiUnavailableException;
import ma.locavite.payment.cmi.exception.CmiWebhookSignatureException;
import ma.locavite.payment.controller.PaymentController;
import ma.locavite.reservation.exception.ReservationNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {PaymentController.class, AdminPaymentController.class, CmiPaymentController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handlePaymentNotFound(PaymentNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ProblemDetail handleReservationNotFound(ReservationNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({AccessDeniedException.class, CmiIpNotAllowedException.class})
    public ResponseEntity<Map<String, Object>> handleAccessDenied(RuntimeException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage() != null ? ex.getMessage() : "Accès refusé");
    }

    @ExceptionHandler(CmiWebhookSignatureException.class)
    public ResponseEntity<Map<String, Object>> handleWebhookSignature(CmiWebhookSignatureException ex) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage() != null ? ex.getMessage() : "Signature CMI invalide");
    }

    @ExceptionHandler(CmiRateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(CmiRateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "60")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("detail", ex.getMessage() != null ? ex.getMessage() : "Limite de requêtes dépassée"));
    }

    @ExceptionHandler({
            InvalidPaymentStatusException.class,
            PaymentValidationException.class,
            CmiSignatureException.class,
            CmiSessionExpiredException.class,
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(CmiUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleCmiUnavailable(CmiUnavailableException ex) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation des données échouée");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(f -> errors.put(f.getField(), f.getDefaultMessage()));
        problem.setProperty("errors", errors);
        return problem;
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String detail) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("detail", detail != null ? detail : status.getReasonPhrase()));
    }
}
