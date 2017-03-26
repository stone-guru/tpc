package net.eric.tpc.net;

import com.google.common.base.Optional;

import net.eric.tpc.common.ErrorMessage;
import net.eric.tpc.common.SysErrorCode;
import net.eric.tpc.proto.Node;

public class PeerResult2 {
    public static enum State {
        PENDING, APPROVED, REFUSED, FAILED
    };

    public static PeerResult2 pending(Node node, Object result) {
        return new PeerResult2(node, State.PENDING, result, null);
    }

    public static PeerResult2 approve(Node node, Object result) {
        return new PeerResult2(node, State.APPROVED, result, null);
    }

    public static PeerResult2 refuse(Node node, String code, String reason) {
        return new PeerResult2(node, State.REFUSED, null, new ErrorMessage(code, reason));
    }

    public static PeerResult2 refuse(Node node, ErrorMessage error) {
        return new PeerResult2(node, State.REFUSED, null, error);
    }

    public static PeerResult2 fail(Node node, String code, String reason) {
        return new PeerResult2(node, State.FAILED, null, new ErrorMessage(code, reason));
    }

    public static PeerResult2 fail(Node node, ErrorMessage error) {
        return new PeerResult2(node, State.FAILED, null, error);
    }

    public static interface Assembler {
        PeerResult2 start(Node node, Object message);

        PeerResult2 fold(Node node, PeerResult2 previous, Object message);

        PeerResult2 finish(Node node, Optional<PeerResult2> previous);
    }

    public static abstract class OneItemAssembler implements Assembler {
        @Override
        public PeerResult2 fold(Node node, PeerResult2 previous, Object message) {
            return previous;
        }

        @Override
        public PeerResult2 finish(Node node, Optional<PeerResult2> previous) {
            if (previous.isPresent()) {
                return previous.get();
            } else {
                return PeerResult2.fail(node, SysErrorCode.NO_RESPONSE, "no data from " + node.toString());
            }
        }
    }

    public static final Assembler ONE_ITEM_ASSEMBLER = new OneItemAssembler() {
        @Override
        public PeerResult2 start(Node node, Object message) {
            return PeerResult2.approve(node, message);
        }
    };
    private Node peer;
    private State state;
    private ErrorMessage error;
    private Object result;

    private PeerResult2(Node peer, State state, Object result, ErrorMessage error) {
        this.peer = peer;
        this.state = state;
        this.result = result;
        this.error = error;
    }

    public boolean hasError() {
        return this.state == State.REFUSED || this.state == State.FAILED;
    }

    public boolean isDone() {
        return this.state != State.PENDING;
    }

    public boolean isRight() {
        return this.state == State.APPROVED;
    }

    public PeerResult2 asDone() {
        if (this.hasError()) {
            throw new IllegalStateException("PeerResult contain error, can not be regard as Done");
        }
        if (this.isDone()) {
            return this;
        }
        return PeerResult2.approve(this.peer, this.result);
    }

    public Node peer() {
        return this.peer;
    }

    public Object result() {
        if (this.hasError()) {
            throw new IllegalStateException("This PeerResult is not right, has no result");
        }
        return this.result;
    }

    public ErrorMessage errorMessage() {
        if (!this.hasError()) {
            throw new IllegalStateException("This PeerResult is right, has no errorCode");
        }
        return this.error;
    }

    public String errorCode() {
        if (!this.hasError()) {
            throw new IllegalStateException("This PeerResult is right, has no errorCode");
        }
        return this.error.getCode();
    }

    public String errorDescription() {
        if (!this.hasError()) {
            throw new IllegalStateException("This PeerResult is right, has no errorDescription");
        }
        return this.error.getDescription();
    }

}
