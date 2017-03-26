package net.eric.tpc.common;

public class BankException extends Exception {
	private static final long serialVersionUID = 1L;
	private ErrorMessage errorMessage;

	public BankException(ErrorMessage errorMessage) {
		super(errorMessage.toString());
		this.errorMessage = errorMessage;
	}

	public BankException(Throwable cause) {
		super(cause);
		this.errorMessage = new ErrorMessage("INNER_EXCEPTION", cause.getMessage());
	}

	public ErrorMessage errorMessage() {
		return this.errorMessage;
	}
}
