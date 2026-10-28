package ma.locavite.vehicle.dto;

import java.time.LocalDateTime;

public record VehiclePhotoResponse(
        Long id,
        Long vehicleId,
        String url,
        String thumbnailUrl,
        boolean isPrimary,
        int ordre,
        LocalDateTime uploadedAt
) {
}
