package net.eric.bank.biz;

import java.util.List;

import net.eric.bank.entity.Account;

public interface AccountRepository {
    void createAccount(Account account);
    
    Account getAccount(String acctId);
    
    List<Account> getAllAccount();
}
