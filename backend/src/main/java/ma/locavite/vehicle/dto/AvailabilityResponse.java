package ma.locavite.vehicle.dto;

import java.io.Serializable;
import java.util.List;

public record AvailabilityResponse(
        List<Period> available,
        List<UnavailablePeriod> unavailable
) implements Serializable {}

