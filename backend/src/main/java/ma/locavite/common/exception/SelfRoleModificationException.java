package ma.locavite.common.exception;

public class SelfRoleModificationException extends RuntimeException {
    public SelfRoleModificationException(String message) {
        super(message);
    }
}
