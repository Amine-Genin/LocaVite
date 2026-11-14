package ma.locavite.payment.event;

import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.Refund;
import ma.locavite.reservation.domain.Reservation;

public record RefundCompletedEvent(
        Refund refund,
        Payment payment,
        Reservation reservation,
        String avoirPath
) {
    public RefundCompletedEvent(Refund refund, Payment payment) {
        this(refund, payment, null, refund != null ? refund.getAvoirPath() : null);
    }
}
