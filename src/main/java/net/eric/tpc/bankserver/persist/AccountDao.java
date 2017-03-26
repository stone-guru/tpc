package net.eric.tpc.bankserver.persist;

import java.math.BigDecimal;

import net.eric.tpc.bankserver.entity.Account;
import net.eric.tpc.common.Pair;

public interface AccountDao {
    void createAccountTable();
    void insert(Account acct); 
    void modifyBalance(Pair<String, BigDecimal> param);
    
}
