package net.eric.tpc.common;

public class ErrorMessage {
	
	public static ErrorMessage create(String code, String description){
		return new ErrorMessage(code, description);
	}
	
	private String code;
	private String description;
	
	public ErrorMessage(String code, String description) {
		super();
		this.code = code;
		this.description = description;
	}

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return "ErrorMessage [code=" + code + ", description=" + description + "]";
	}
}
