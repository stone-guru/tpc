package net.eric.tpc.common;

public class BankException extends Exception {
    private static final long serialVersionUID = 1L;
    private ActionStatus errorMessage;

    public BankException(ActionStatus errorMessage) {
        super(errorMessage.toString());
        this.errorMessage = errorMessage;
    }

    public BankException(Throwable cause) {
        super(cause);
        this.errorMessage = new ActionStatus("INNER_EXCEPTION", cause.getMessage());
    }

    public ActionStatus errorMessage() {
        return this.errorMessage;
    }
}
