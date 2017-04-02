package net.eric.tpc.net;

import com.google.common.base.Optional;

import net.eric.tpc.base.Pair;
import net.eric.tpc.proto.PeerTransactionState;

public class HeartBeatRequestHandler<B> implements RequestHandler<B> {
    private static final Optional<DataPacket> heartbeatAnswer = //
            Optional.of(DataPacket.readOnlyPacket(DataPacket.HEART_BEAT_ANSWER, "", ""));

    public String getCorrespondingCode() {
        return DataPacket.HEART_BEAT;
    }

    @Override
    public boolean requireTransState() {
        return false;
    }

    @Override
    public Pair<Optional<DataPacket>, Boolean>  process(DataPacket request, PeerTransactionState<B> state) {
        return Pair.asPair(HeartBeatRequestHandler.heartbeatAnswer, false);
    }
}
