package ma.locavite.vehicle.exception;

public class VehicleNotFoundException extends RuntimeException {
    public VehicleNotFoundException(Long id) {
        super("Véhicule introuvable : " + id);
    }
}
