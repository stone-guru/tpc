package net.eric.tpc.common;

public class BankException extends Exception {
    private static final long serialVersionUID = 1L;
    private ActionResult errorMessage;

    public BankException(ActionResult errorMessage) {
        super(errorMessage.toString());
        this.errorMessage = errorMessage;
    }

    public BankException(Throwable cause) {
        super(cause);
        this.errorMessage = new ActionResult("INNER_EXCEPTION", cause.getMessage());
    }

    public ActionResult errorMessage() {
        return this.errorMessage;
    }
}
