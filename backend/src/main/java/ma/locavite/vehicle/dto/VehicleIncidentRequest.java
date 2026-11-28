package ma.locavite.vehicle.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public record VehicleIncidentRequest(@NotNull LocalDate date, @NotBlank @Size(max = 1000) String description,
        @NotBlank @Size(max = 20) String gravite) {}
