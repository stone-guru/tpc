package net.eric.tpc.coor.stub;

import static net.eric.tpc.common.Pair.asPair;

import java.util.concurrent.Future;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;

import net.eric.tpc.biz.BizCode;
import net.eric.tpc.common.ActionStatus;
import net.eric.tpc.common.Configuration;
import net.eric.tpc.common.Either;
import net.eric.tpc.common.Maybe;
import net.eric.tpc.common.Node;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.proto.CoorBizStrategy;

public class AbcBizStrategy implements CoorBizStrategy<TransferBill> {

    private Configuration config = new Configuration();

    @Override
    public Maybe<TaskPartition<TransferBill>> splitTask(String xid, TransferBill b) {
        ActionStatus checkResult = basicCheck(b);
        if (!checkResult.isOK()) {
            return Maybe.fail(checkResult);
        }

        Node boc = config.getNode("BOC").get();
        Node ccb = config.getNode("CCB").get();
        return Maybe.success(//
                new TaskPartition<TransferBill>(b, //
                        ImmutableList.of(asPair(boc, b), asPair(ccb, b))));
    }

    public ActionStatus basicCheck(TransferBill bill) {
        ActionStatus checkResult = bill.fieldCheck();
        if (!checkResult.isOK()) {
            return checkResult;
        }
        if (!config.isBankNodeExists(bill.getAccount().getBankCode())) {
            return ActionStatus.create(BizCode.NO_BANK_NODE, bill.getAccount().getBankCode());
        }

        if (!config.isBankNodeExists(bill.getOppositeAccount().getBankCode())) {
            return ActionStatus.create(BizCode.NO_BANK_NODE, bill.getOppositeAccount().getBankCode());
        }

        return ActionStatus.OK;
    }

    @Override
    public Future<ActionStatus> prepareCommit(String xid, TransferBill b) {
        // FIXME
        return Futures.immediateFuture(ActionStatus.OK);
    }

    @Override
    public Future<ActionStatus> commit(TransferBill b) {
        // TODO Auto-generated method stub
        return null;
    }
}
