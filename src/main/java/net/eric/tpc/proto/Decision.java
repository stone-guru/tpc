package net.eric.tpc.proto;

public enum Decision {
    COMMIT("COMMIT"), ABORT("ABORT");
    
    public static Decision fromCode(String code){
        for(Decision v : Decision.values()){
            if (v.code().equalsIgnoreCase(code)){
                return v;
            }
        }
        return null;
    }
    
    private Decision(String s) {
        this.code = s;
    }

    public String code() {
        return this.code;
    }

    private String code;
}
