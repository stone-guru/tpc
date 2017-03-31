package net.eric.tpc.persist;

import net.eric.tpc.base.Pair;
import net.eric.tpc.entity.TransferBill;

public interface TransferBillDao {
    void insert(TransferBill bill);

    void updateLock(Pair<String, String> snAndXid);
    
    TransferBill selectByXid(String xid);
    
    void deleteByXid(String xid);
    
    void clearLockByXid(String xid);
}
