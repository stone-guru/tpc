package net.eric.tpc.proto;

import net.eric.tpc.common.ActionResult;
import net.eric.tpc.common.BankException;

public interface CoorTransManager<B> {
    ActionResult  transaction(B biz) throws BankException;
}
