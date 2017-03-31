package net.eric.tpc.net;

import com.google.common.base.Optional;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Node;
import net.eric.tpc.base.SysErrorCode;

public class PeerResult {
    public static enum State {
        PENDING, APPROVED, REFUSED, FAILED
    };

    public static PeerResult pending(Node node, Object result) {
        return new PeerResult(node, State.PENDING, result, null);
    }

    public static PeerResult approve(Node node, Object result) {
        return new PeerResult(node, State.APPROVED, result, null);
    }

    public static PeerResult refuse(Node node, String code, String reason) {
        return new PeerResult(node, State.REFUSED, null, new ActionStatus(code, reason));
    }

    public static PeerResult refuse(Node node, ActionStatus error) {
        return new PeerResult(node, State.REFUSED, null, error);
    }

    public static PeerResult fail(Node node, String code, String reason) {
        return new PeerResult(node, State.FAILED, null, new ActionStatus(code, reason));
    }

    public static PeerResult fail(Node node, ActionStatus error) {
        return new PeerResult(node, State.FAILED, null, error);
    }

    public static interface Assembler {
        PeerResult start(Node node, Object message);

        PeerResult fold(Node node, PeerResult previous, Object message);

        PeerResult finish(Node node, Optional<PeerResult> previous);
    }

    public static abstract class OneItemAssembler implements Assembler {
        @Override
        public PeerResult fold(Node node, PeerResult previous, Object message) {
            return previous;
        }

        @Override
        public PeerResult finish(Node node, Optional<PeerResult> previous) {
            if (previous.isPresent()) {
                return previous.get();
            } else {
                return PeerResult.fail(node, SysErrorCode.NO_RESPONSE, "no data from " + node.toString());
            }
        }
    }

    public static final Assembler ONE_ITEM_ASSEMBLER = new OneItemAssembler() {
        @Override
        public PeerResult start(Node node, Object message) {
            return PeerResult.approve(node, message);
        }
    };
    private Node peer;
    private State state;
    private ActionStatus error;
    private Object result;

    private PeerResult(Node peer, State state, Object result, ActionStatus error) {
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

    public PeerResult asDone() {
        if (this.hasError()) {
            throw new IllegalStateException("PeerResult contain error, can not be regard as Done");
        }
        if (this.isDone()) {
            return this;
        }
        return PeerResult.approve(this.peer, this.result);
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

    public ActionStatus errorMessage() {
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

    @Override
    public String toString() {
        return "PeerResult[peer=" + peer + ", state=" + state + ", error=" + error + ", result=" + result + "]";
    }

    
}
