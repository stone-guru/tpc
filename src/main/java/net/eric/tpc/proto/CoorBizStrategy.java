package net.eric.tpc.proto;

import java.util.List;

import com.google.common.base.Optional;

import net.eric.tpc.common.BankException;
import net.eric.tpc.common.ActionResult;
import net.eric.tpc.common.Pair;

public interface CoorBizStrategy<B> {

    List<Pair<Node, B>> splitTask(B b) throws BankException;

    ActionResult checkTransRequest(B b);
}
