package ma.locavite.payment.cmi.dto;

import ma.locavite.payment.domain.PaymentStatus;

public record CmiWebhookResponse(
        String action,
        boolean duplicate,
        Long paymentId,
        PaymentStatus status
) {
}
