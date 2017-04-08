package net.eric.bank.persist;

import java.math.BigDecimal;
import java.util.List;

import net.eric.bank.entity.Account;
import net.eric.tpc.base.Pair;

public interface AccountDao {

    void insert(Account acct);

    void modifyBalance(Pair<String, BigDecimal> param);
    
    void updateLock(Pair<String, Long> param);
    
    Account selectByAcctNumber(String acctNumber);
    
    List<Account> selectAll();

}
