package net.eric.tpc.persist;

import java.math.BigDecimal;
import java.util.List;

import net.eric.tpc.base.Pair;
import net.eric.tpc.entity.Account;

public interface AccountDao {

    void insert(Account acct);

    void modifyBalance(Pair<String, BigDecimal> param);
    
    void updateLock(Pair<String, String> param);
    
    Boolean isAccountLocked(String acctNumber);
    
    Account selectByAcctNumber(String acctNumber);
    
    List<Account> selectAll();

}
