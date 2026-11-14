package ma.locavite.payment.event;

import ma.locavite.payment.domain.Payment;

public record PaymentSucceededEvent(Payment payment) {}
