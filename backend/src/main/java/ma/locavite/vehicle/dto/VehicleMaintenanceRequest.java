package ma.locavite.vehicle.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public record VehicleMaintenanceRequest(@NotNull LocalDate date, @NotBlank @Size(max = 500) String description,
        @NotNull @PositiveOrZero Integer kilometrage, LocalDate prochaineMaintenance) {}
