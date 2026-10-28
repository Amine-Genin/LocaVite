package ma.locavite.vehicle.service;

import ma.locavite.vehicle.exception.InvalidVehiclePhotoException;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ImageCompressionService {

    public static final int THUMBNAIL_SIZE = 300;

    public byte[] createThumbnail(byte[] imageBytes) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Thumbnails.of(new ByteArrayInputStream(imageBytes))
                    .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                    .crop(Positions.CENTER)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(output);
            return output.toByteArray();
        } catch (IOException | RuntimeException e) {
            throw new InvalidVehiclePhotoException("Impossible de traiter cette image", e);
        }
    }
}
