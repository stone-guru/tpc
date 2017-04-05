package net.eric.tpc.net;

import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;

import com.google.common.base.Optional;

import net.eric.tpc.proto.PeerTransactionState;

public abstract class TransactionRequestHandler<B> implements RequestHandler {
    public static AttributeKey transStateKey = new AttributeKey(TransactionRequestHandler.class, "TRANSACTION_STATE");
    
    public  static <B> Optional<PeerTransactionState<B>> getTransactionState(IoSession session) {
        @SuppressWarnings("unchecked")
        PeerTransactionState<B> state = (PeerTransactionState<B>) session.getAttribute(TransactionRequestHandler.transStateKey);
        return Optional.fromNullable(state);
    }

    public  static <B> void putTransactionState(IoSession session, PeerTransactionState<B> state ) {
        session.setAttribute(TransactionRequestHandler.transStateKey, state);
    }
    
    public boolean requireTransState() {
        return true;
    }
    
    @Override
    public ProcessResult process(IoSession session, DataPacket request) {
        Optional<PeerTransactionState<B>> opt = TransactionRequestHandler.getTransactionState(session);
        if (this.requireTransState() && !opt.isPresent()) {
            return ProcessResult.NO_RESPONSE_AND_CLOSE;
        }

        return this.process(request, opt.orNull());
    }

    protected abstract ProcessResult process(DataPacket request, PeerTransactionState<B> state);

}
