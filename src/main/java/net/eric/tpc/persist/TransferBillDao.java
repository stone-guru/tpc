package net.eric.tpc.persist;

import net.eric.tpc.common.Pair;
import net.eric.tpc.entity.TransferBill;

public interface TransferBillDao {
    void insert(TransferBill bill);
    void updateLock(Pair<String, String> snAndXid);
}
