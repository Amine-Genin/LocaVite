package ma.locavite.sale.exception;

public class VinAlreadyExistsException extends RuntimeException {

    public VinAlreadyExistsException(String vin) {
        super("Un véhicule avec le numéro de châssis (VIN) " + vin + " existe déjà.");
    }
}
