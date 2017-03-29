package net.eric.tpc.biz;

import java.util.List;

import net.eric.tpc.entity.Account;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.proto.PeerBizStrategy;

public interface AccountRepository extends PeerBizStrategy<TransferBill>{
    void createAccount(Account account);
    
    Account getAccount(String acctId);
    
    List<Account> getAllAccount();
}
