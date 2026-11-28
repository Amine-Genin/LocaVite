package ma.locavite.vehicle.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.locavite.vehicle.domain.VehicleStatus;
public record VehicleStatusRequest(@NotNull VehicleStatus statut, @Size(max = 500) String raison) {}
