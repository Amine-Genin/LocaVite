package ma.locavite.reservation.service;

import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.repository.RefundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private RefundRepository refundRepository;

    private final RefundService refundService = new RefundService(refundRepository);

    @Test
    void calculatesFullRefundTenDaysBeforeStart() {
        assertThat(refundFor(10)).isEqualByComparingTo("1000.00");
    }

    @Test
    void calculatesSeventyPercentFiveDaysBeforeStart() {
        assertThat(refundFor(5)).isEqualByComparingTo("700.00");
    }

    @Test
    void calculatesFiftyPercentTwoDaysBeforeStart() {
        assertThat(refundFor(2)).isEqualByComparingTo("500.00");
    }

    @Test
    void calculatesNoRefundOnStartDate() {
        assertThat(refundFor(0)).isEqualByComparingTo("0.00");
    }

    private BigDecimal refundFor(long daysBeforeStart) {
        Reservation reservation = new Reservation();
        reservation.setPrixTotal(new BigDecimal("1000.00"));
        reservation.setDateDebut(LocalDate.now().plusDays(daysBeforeStart));
        return refundService.calculateRefund(reservation);
    }
}
