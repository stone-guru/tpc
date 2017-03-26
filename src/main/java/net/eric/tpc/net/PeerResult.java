package net.eric.tpc.net;

import net.eric.tpc.proto.Node;

public class PeerResult {
    private boolean hasError;
    private boolean isDone;
    private String errorCode;
    private String errorReason;
    private Object result;
    private Node peer;

    public PeerResult(Node peer, Object result){
        this(peer, result, true);
    }
    
    public PeerResult(Node peer, Object result, boolean isDone) {
        assert (peer != null);
        if (result == null) {
            throw new NullPointerException("result can not be null");
        }

        this.hasError = false;
        this.isDone = isDone;
        this.peer = peer;
        this.result = result;
    }

    public PeerResult(Node peer, String errorCode, String errorReason) {
        if (errorCode == null) {
            throw new NullPointerException("errorCode can not be null");
        }

        this.hasError = true;
        this.isDone = true;
        this.peer = peer;
        this.errorCode = errorCode;
        this.errorReason = errorReason;
    }

    public PeerResult asDone(){
        if(this.hasError){
            throw new IllegalStateException("PeerResult contain error, can not be regard as Done");
        }
        if(this.isDone){
            return this;
        }
        return new PeerResult(this.peer, this.result);
    }
    
    public Node peer() {
        return this.peer;
    }

    public boolean isDone(){
        return this.isDone;
    }
    
    public boolean isRight() {
        return !this.hasError && this.isDone;
    }

    public String errorCode() {
        if (!this.hasError) {
            throw new IllegalStateException("This CommuResult is right, has no errorCode");
        }
        return this.errorCode;
    }

    public String errorReason() {
        if (!this.hasError) {
            throw new IllegalStateException("This CommuResult is right, has no errorReason");
        }
        return this.errorReason;
    }

    public Object result() {
        if (this.hasError) {
            throw new IllegalStateException("This CommuResult is not right, has no result");
        }
        return this.result;
    }
}