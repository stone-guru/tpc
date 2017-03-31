package net.eric.tpc.biz;

import java.util.List;

import net.eric.tpc.entity.Account;

public interface AccountRepository {
    void createAccount(Account account);
    
    Account getAccount(String acctId);
    
    List<Account> getAllAccount();
}
