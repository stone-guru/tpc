package net.eric.tpc.entity;

public enum AccountType {
    PERSONAL("PERSONAL"), BANK("BANK");

    public static AccountType fromCode(String code){
        for(AccountType v : AccountType.values()){
            if (v.code().equalsIgnoreCase(code)){
                return v;
            }
        }
        return null;
    }
    
    private AccountType(String s) {
        this.code = s;
    }

    public String code() {
        return this.code;
    }

    private String code;
}
