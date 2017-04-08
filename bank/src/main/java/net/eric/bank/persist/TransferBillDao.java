package net.eric.bank.persist;

import net.eric.bank.entity.TransferBill;
import net.eric.tpc.base.Pair;

public interface TransferBillDao {
    void insert(TransferBill bill);

    void updateLock(Pair<String, Long> snAndXid);
    
    TransferBill selectByXid(long xid);
    
    void deleteByXid(long xid);
    
    void clearLockByXid(long xid);
}
