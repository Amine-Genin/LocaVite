package ma.locavite.vehicle.exception;

public class VehicleNotFoundException extends RuntimeException {
    public VehicleNotFoundException(Long vehicleId) {
        super("Véhicule non trouvé avec l'id : " + vehicleId);
    }
}
