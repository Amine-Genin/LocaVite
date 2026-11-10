package ma.locavite.reservation.service;

import ma.locavite.reservation.config.ContractProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class PdfFontPathResolver {

    private final ContractProperties properties;

    public PdfFontPathResolver(ContractProperties properties) {
        this.properties = properties;
    }

    public String resolve() {
        String configuredPath = properties.getFontPath();
        if (configuredPath != null && !configuredPath.isBlank()) {
            if (Files.isRegularFile(Path.of(configuredPath))) {
                return configuredPath;
            }
            throw new IllegalStateException("Le fichier CONTRACT_FONT_PATH configuré est introuvable");
        }
        for (String candidate : List.of(
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/ttf-dejavu/DejaVuSans.ttf",
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/Arial.ttf")) {
            if (Files.isRegularFile(Path.of(candidate))) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Police Unicode introuvable. Installez DejaVu Sans ou configurez CONTRACT_FONT_PATH");
    }
}
