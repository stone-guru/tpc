package net.eric.tpc.biz;

public enum TransType {
	INCOME("I"), PAYMENT("P");
	
	public static TransType fromCode(String code){
		for(TransType v : TransType.values()){
			if (v.code().equalsIgnoreCase(code)){
				return v;
			}
		}
		return null;
	}
	
	private TransType(String s) {
		this.code = s;
	}

	public String code() {
		return this.code;
	}

	private String code;

}
