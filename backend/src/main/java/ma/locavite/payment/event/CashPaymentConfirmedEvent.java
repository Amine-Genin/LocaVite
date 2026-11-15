package ma.locavite.payment.event;

import ma.locavite.payment.domain.Payment;
import ma.locavite.reservation.domain.Reservation;

public record CashPaymentConfirmedEvent(Payment payment, Reservation reservation) {}
