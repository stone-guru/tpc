package net.eric.tpc.proto;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.List;

import com.google.common.base.Optional;

import net.eric.tpc.proto.Types.Decision;

public interface DecisionQuerier extends Closeable{
    Optional<Decision> queryDecision(long xid, List<InetSocketAddress> peers);
}
