package ma.locavite.vehicle.exception;

public class MatriculeAlreadyExistsException extends RuntimeException {
    public MatriculeAlreadyExistsException(String matricule) {
        super("Un véhicule avec le matricule '" + matricule + "' existe déjà");
    }
}