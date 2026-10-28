package ma.locavite.catalog.exception;

public class DuplicateCatalogEntryException extends RuntimeException {

    public DuplicateCatalogEntryException(String name) {
        super("Une entrée de catalogue existe déjà avec le nom : " + name);
    }
}
