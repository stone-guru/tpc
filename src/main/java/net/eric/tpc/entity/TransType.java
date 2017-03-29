package net.eric.tpc.entity;

public enum TransType {
    INCOME("INCOME"), PAYMENT("PAYMENT");
    
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
