package net.eric.tpc.proto;

import java.util.List;

import net.eric.tpc.common.Pair;

public interface BizStrategy<B> {
	List<Pair<Node, B>> splitTask(B b);
}
