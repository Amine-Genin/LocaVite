package ma.locavite.vehicle.dto;

import java.io.Serializable;
import java.time.LocalDate;

public record Period(
        LocalDate from,
        LocalDate to
) implements Serializable {}

