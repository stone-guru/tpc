package net.eric.tpc.persist;

import net.eric.tpc.base.Pair;
import net.eric.tpc.entity.TransferBill;

public interface TransferBillDao {
    void insert(TransferBill bill);

    void updateLock(Pair<String, Long> snAndXid);
    
    TransferBill selectByXid(long xid);
    
    void deleteByXid(long xid);
    
    void clearLockByXid(long xid);
}
