package net.eric.tpc.common;

public class UnImplementedException extends RuntimeException {

    private static final long serialVersionUID = -2099379371619165790L;

    public UnImplementedException() {
        super();
    }

    public UnImplementedException(String message) {
        super(message);
    }
}
