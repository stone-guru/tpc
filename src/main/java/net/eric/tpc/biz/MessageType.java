package net.eric.tpc.biz;

public enum MessageType {
    NORMAL("N"), REVERSE("R");

    public static MessageType fromCode(String code){
        for(MessageType v : MessageType.values()){
            if (v.code().equalsIgnoreCase(code)){
                return v;
            }
        }
        return null;
    }
    
    private MessageType(String s) {
        this.code = s;
    }

    public String code() {
        return this.code;
    }

    private String code;

}
