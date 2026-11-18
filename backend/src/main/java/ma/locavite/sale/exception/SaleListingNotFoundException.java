package ma.locavite.sale.exception;

public class SaleListingNotFoundException extends RuntimeException {

    public SaleListingNotFoundException(Long id) {
        super("Annonce de vente introuvable : " + id);
    }

    public SaleListingNotFoundException(String message) {
        super(message);
    }
}
