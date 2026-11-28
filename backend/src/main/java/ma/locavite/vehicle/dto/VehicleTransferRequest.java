package ma.locavite.vehicle.dto;
import jakarta.validation.constraints.NotNull;
public record VehicleTransferRequest(@NotNull Long agenceId) {}
