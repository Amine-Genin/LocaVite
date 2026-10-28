package ma.locavite.vehicle.dto;

import java.io.Serializable;
import java.time.LocalDate;

public record UnavailablePeriod(
        LocalDate from,
        LocalDate to,
        String reason
) implements Serializable {}

