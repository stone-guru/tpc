package net.eric.tpc.common;

public class ShouldNotHappenException extends RuntimeException {

    private static final long serialVersionUID = 4293183055502330856L;

    public ShouldNotHappenException() {
        super();
    }

    public ShouldNotHappenException(String msg){
        super(msg);
    }
    
    public ShouldNotHappenException(Exception e) {
        super(e);
    }

}
