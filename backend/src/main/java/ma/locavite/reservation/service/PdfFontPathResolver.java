package ma.locavite.reservation.service;

import ma.locavite.reservation.config.ContractProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class PdfFontPathResolver {

    private static final List<Path> FIXED_FONT_DIRECTORIES = List.of(
            Path.of("/usr/share/fonts/truetype/dejavu"),
            Path.of("/usr/share/fonts/ttf-dejavu"),
            Path.of("C:/Windows/Fonts"));

    private final ContractProperties properties;

    public PdfFontPathResolver(ContractProperties properties) {
        this.properties = properties;
    }

    public String resolve() {
        String configuredPath = properties.getFontPath();
        if (configuredPath != null && !configuredPath.isBlank()) {
            Path path = Path.of(configuredPath).normalize().toAbsolutePath();
            if (Files.isRegularFile(path) && isAllowedFontPath(path)) {
                return path.toString();
            }
            throw new IllegalStateException(
                    "Le chemin CONTRACT_FONT_PATH doit pointer vers un fichier de police système autorisé");
        }
        for (String candidate : List.of(
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/ttf-dejavu/DejaVuSans.ttf",
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/Arial.ttf")) {
            Path candidatePath = Path.of(candidate).normalize().toAbsolutePath();
            if (Files.isRegularFile(candidatePath) && isAllowedFontPath(candidatePath)) {
                return candidatePath.toString();
            }
        }
        throw new IllegalStateException(
                "Police Unicode introuvable. Installez DejaVu Sans ou configurez CONTRACT_FONT_PATH");
    }

    private boolean isAllowedFontPath(Path candidate) {
        Path normalized = candidate.normalize();
        return FIXED_FONT_DIRECTORIES.stream()
                .map(Path::normalize)
                .anyMatch(base -> normalized.startsWith(base));
    }
}
