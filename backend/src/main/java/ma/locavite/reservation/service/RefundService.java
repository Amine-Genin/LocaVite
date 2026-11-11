package ma.locavite.reservation.service;

import ma.locavite.reservation.domain.Refund;
import ma.locavite.reservation.domain.RefundStatus;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class RefundService {

    private final RefundRepository refundRepository;

    public RefundService(RefundRepository refundRepository) {
        this.refundRepository = refundRepository;
    }

    public BigDecimal calculateRefund(Reservation reservation) {
        long daysBeforeStart = ChronoUnit.DAYS.between(LocalDate.now(), reservation.getDateDebut());
        BigDecimal rate = daysBeforeStart > 7 ? BigDecimal.ONE
                : daysBeforeStart >= 3 ? new BigDecimal("0.70")
                : daysBeforeStart > 0 ? new BigDecimal("0.50")
                : BigDecimal.ZERO;
        return reservation.getPrixTotal().multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateRefundRate(Reservation reservation) {
        if (reservation.getPrixTotal().signum() == 0) {
            return BigDecimal.ZERO;
        }
        return calculateRefund(reservation).divide(reservation.getPrixTotal(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    @Transactional
    public Refund create(Reservation reservation) {
        Refund refund = new Refund();
        refund.setReservation(reservation);
        refund.setMontant(calculateRefund(reservation));
        refund.setRaison("Annulation à " + Math.max(0,
                ChronoUnit.DAYS.between(LocalDate.now(), reservation.getDateDebut())) + " jour(s) du début");
        refund.setStatut(RefundStatus.MANUAL);
        return refundRepository.save(refund);
    }
}
