package ma.locavite.reservation.domain;

import java.math.BigDecimal;

public enum ReservationOption {
    GPS(new BigDecimal("50.00")),
    BABY_SEAT(new BigDecimal("50.00")),
    EXTRA_DRIVER(new BigDecimal("100.00"));

    private final BigDecimal price;

    ReservationOption(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
