package net.eric.tpc.coor.stub;

import static net.eric.tpc.common.Pair.asPair;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.Future;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;

import net.eric.tpc.biz.BizErrorCode;
import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.common.ActionResult;
import net.eric.tpc.common.Configuration;
import net.eric.tpc.common.Either;
import net.eric.tpc.proto.CoorBizStrategy;
import net.eric.tpc.proto.Node;

public class AbcBizStrategy implements CoorBizStrategy<TransferMessage> {

    private Configuration config = new Configuration();

    public Either<ActionResult, TaskPartition<TransferMessage>> splitTask(TransferMessage b) {
        ActionResult checkResult = basicCheckTransRequest(b);
        if (!checkResult.isOK()) {
            return Either.left(checkResult);
        }

        Node boc = config.getNode("BOC").get();
        Node ccb = config.getNode("CCB").get();
        return Either.right(//
                new TaskPartition<TransferMessage>(b, //
                        ImmutableList.of(asPair(boc, b), asPair(ccb, b))));
    }

    
    private ActionResult basicCheckTransRequest(TransferMessage m) {
        ActionResult r = checkFieldMissing(m);
        if (!r.isOK()) {
            return r;
        }

        if (!config.isBankNodeExists(m.getAccount().getBankCode())) {
            return ActionResult.create(BizErrorCode.NO_BANK_NODE, m.getAccount().getBankCode());
        }

        if (!config.isBankNodeExists(m.getOppositeAccount().getBankCode())) {
            return ActionResult.create(BizErrorCode.NO_BANK_NODE, m.getOppositeAccount().getBankCode());
        }

        long now = new Date().getTime();
        if (m.getLaunchTime().getTime() > now) {
            return ActionResult.create(BizErrorCode.TIME_IS_FUTURE, m.getLaunchTime().toString());
        }

        if (m.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ActionResult.create(BizErrorCode.AMOUNT_LE_ZERO, m.getAmount().toString());
        }

        if (m.getAccount().equals(m.getOppositeAccount())) {
            return ActionResult.create(BizErrorCode.SAME_ACCOUNT, m.getAccount().toString());
        }

        return ActionResult.OK;
    }

    private ActionResult checkFieldMissing(TransferMessage m) {
        if (m == null) {
            throw new NullPointerException("TransferMessage is null");
        }

        if (Strings.isNullOrEmpty(m.getTransSN())) {
            return ActionResult.create(BizErrorCode.MISS_FIELD, "TransSN");
        }

        if (m.getLaunchTime() == null) {
            return ActionResult.create(BizErrorCode.MISS_FIELD, "launchTime");
        }

        if (Strings.isNullOrEmpty(m.getReceivingBankCode())) {
            return ActionResult.create(BizErrorCode.MISS_FIELD, "ReceivingBankCode");
        }

        if (Strings.isNullOrEmpty(m.getVoucherNumber())) {
            return ActionResult.create(BizErrorCode.MISS_FIELD, "VoucherNumber");
        }

        if (m.getAccount() == null) {
            return ActionResult.create(BizErrorCode.MISS_FIELD, "Account");
        }

        if (Strings.isNullOrEmpty(m.getAccount().getBankCode())) {
            return ActionResult.create(BizErrorCode.MISS_FIELD, "Account.BankCode");
        }

        if (Strings.isNullOrEmpty(m.getAccount().getNumber())) {
            return ActionResult.create(BizErrorCode.MISS_FIELD, "Account.Number");
        }

        if (m.getOppositeAccount() == null) {
            return ActionResult.create(BizErrorCode.MISS_FIELD, "OppositeAccount");
        }

        if (Strings.isNullOrEmpty(m.getOppositeAccount().getBankCode())) {
            return ActionResult.create(BizErrorCode.MISS_FIELD, "OppositeAccount.BankCode");
        }

        if (Strings.isNullOrEmpty(m.getOppositeAccount().getNumber())) {
            return ActionResult.create(BizErrorCode.MISS_FIELD, "OppositeAccount.Number");
        }

        if (m.getAmount() == null) {
            return ActionResult.create(BizErrorCode.MISS_FIELD, "Amount");
        }

        if (m.getTransType() == null) {
            return ActionResult.create(BizErrorCode.MISS_FIELD, "TransType");
        }

        if (Strings.isNullOrEmpty(m.getSummary())) {
            return ActionResult.create(BizErrorCode.MISS_FIELD, "Summary");
        }

        return ActionResult.OK;
    }

    @Override
    public Future<ActionResult> canCommit(TransferMessage b) {
        // FIXME
        return Futures.immediateFuture(ActionResult.OK);
    }

    @Override
    public Future<ActionResult> commit(TransferMessage b) {
        // TODO Auto-generated method stub
        return null;
    }
}
