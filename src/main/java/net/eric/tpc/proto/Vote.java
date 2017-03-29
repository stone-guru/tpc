package net.eric.tpc.proto;

public enum Vote {
    YES("YES"), NO("NO");
       
    public static Vote fromCode(String code){
        for(Vote v : Vote.values()){
            if (v.code().equalsIgnoreCase(code)){
                return v;
            }
        }
        return null;
    }
    
    private Vote(String s) {
        this.code = s;
    }

    public String code() {
        return this.code;
    }

    private String code;
}
