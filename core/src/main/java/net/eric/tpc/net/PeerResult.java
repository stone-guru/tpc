package net.eric.tpc.net;

import java.net.InetSocketAddress;

import com.google.common.base.Optional;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.proto.Types.ErrorCode;

public class PeerResult {
    public static enum State {
        PENDING, APPROVED, REFUSED, FAILED
    };

    public static PeerResult pending(InetSocketAddress node, Object result) {
        return new PeerResult(node, State.PENDING, result, null);
    }

    public static PeerResult approve(InetSocketAddress node, Object result) {
        return new PeerResult(node, State.APPROVED, result, null);
    }

    public static PeerResult refuse(InetSocketAddress node, short code, String reason) {
        return new PeerResult(node, State.REFUSED, null, new ActionStatus(code, reason));
    }

    public static PeerResult refuse(InetSocketAddress node, ActionStatus error) {
        return new PeerResult(node, State.REFUSED, null, error);
    }

    public static PeerResult fail(InetSocketAddress node, short code, String reason) {
        return new PeerResult(node, State.FAILED, null, new ActionStatus(code, reason));
    }

    public static PeerResult fail(InetSocketAddress node, ActionStatus error) {
        return new PeerResult(node, State.FAILED, null, error);
    }

    public static interface Assembler {
        PeerResult start(InetSocketAddress node, Object message);

        PeerResult fold(InetSocketAddress node, PeerResult previous, Object message);

        PeerResult finish(InetSocketAddress node, Optional<PeerResult> previous);
    }

    public static abstract class OneItemAssembler implements Assembler {
        @Override
        public PeerResult fold(InetSocketAddress node, PeerResult previous, Object message) {
            return previous;
        }

        @Override
        public PeerResult finish(InetSocketAddress node, Optional<PeerResult> previous) {
            if (previous.isPresent()) {
                return previous.get();
            } else {
                return PeerResult.fail(node, ErrorCode.NO_RESPONSE, "no data from " + node.toString());
            }
        }
    }

    public static final Assembler ONE_ITEM_ASSEMBLER = new OneItemAssembler() {
        @Override
        public PeerResult start(InetSocketAddress node, Object message) {
            return PeerResult.approve(node, message);
        }
    };
    private InetSocketAddress peer;
    private State state;
    private ActionStatus error;
    private Object result;

    private PeerResult(InetSocketAddress peer, State state, Object result, ActionStatus error) {
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

    public InetSocketAddress peer() {
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

    public short errorCode() {
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
