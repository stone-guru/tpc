package net.eric.tpc.bankserver.persist;

public class PersistException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PersistException() {
        super();
    }

    public PersistException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public PersistException(String message, Throwable cause) {
        super(message, cause);
    }

    public PersistException(String message) {
        super(message);
    }

    public PersistException(Throwable cause) {
        super(cause);
    }
}
