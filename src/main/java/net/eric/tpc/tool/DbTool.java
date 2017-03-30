package net.eric.tpc.tool;

import static net.eric.tpc.common.Pair.asPair;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import net.eric.tpc.common.Pair;
import net.eric.tpc.entity.Account;
import net.eric.tpc.entity.AccountType;
import net.eric.tpc.persist.AccountDao;
import net.eric.tpc.persist.DatabaseInit;
import net.eric.tpc.persist.PersisterFactory;

public class DbTool {
    private static final String USAGE = "Usage: net.eric.tpc.tool.DbInit code directory\n" //
            + "code : should within (BOC, CCB, CBRC, ABC)\n "//
            + "directory : a exist directory to contain the datafile , like d:/database or ./database/bank.";

    private static Set<String> codeSet = ImmutableSet.of("BOC", "CCB", "CBRC", "ABC");
    
    public static void main(String[] args) {
        DbTool tool = new DbTool();
        Optional<Pair<String, String>> opt = tool.parseArgument(args);
        if(!opt.isPresent()){
            System.out.println(USAGE);
            return;
        }
        
        tool.initDatabase(opt.get().fst(), opt.get().snd());
    }

    private Optional<Pair<String, String>> parseArgument(String[] args){
        if(args == null || args.length != 2){
            return Optional.absent();
        }
        if(args[0] == null || args[1] == null){
            return Optional.absent();
        }
        
        String code = args[0].toUpperCase();
        if(!codeSet.contains(code)){
            return Optional.absent();
        }
        return Optional.of(asPair(code, args[1]));
    }
    
    private void initDatabase(String bankCode, String dirname){
        PersisterFactory.initialize("jdbc:h2:" + dirname + "/data_" + bankCode.toLowerCase());
        DatabaseInit dbInit = PersisterFactory.getMapper(DatabaseInit.class);
 
        if(bankCode.equals("BOC") || bankCode.equals("CCB")){
            dbInit.dropAccountTable();
            dbInit.createAccountTable();
            this.insertAccounts(bankCode, PersisterFactory.getMapper(AccountDao.class));    
        }
        
        dbInit.dropTransferBillTable();
        dbInit.createTransferBillTable();
        
        dbInit.dropDtTable();
        dbInit.createDtTable();
     }
    
    private void insertAccounts(String bankCode, AccountDao accountDao){
        Date current = new Date();
        
        Account james = new Account();
        james.setType(AccountType.PERSONAL);
        james.setAcctNumber("james");
        james.setAcctName("James.Brown");
        james.setBalance(BigDecimal.valueOf(1000L));
        james.setOverdraftLimit(BigDecimal.valueOf(1000L));
        james.setBankCode(bankCode);
        james.setOpeningTime(current);
        james.setLastModiTime(current);
        accountDao.insert(james);
        
        Account ryan = new Account();
        ryan.setType(AccountType.PERSONAL);
        ryan.setAcctName("Ryan.White");
        ryan.setAcctNumber("ryan");
        ryan.setBalance(BigDecimal.valueOf(1000L));
        ryan.setOverdraftLimit(BigDecimal.valueOf(4000L));        
        ryan.setBankCode(bankCode);
        ryan.setOpeningTime(current);
        ryan.setLastModiTime(current);
        accountDao.insert(ryan);
        
        Account betty = new Account();
        betty.setType(AccountType.PERSONAL);
        betty.setAcctName("Betty.Moore");
        betty.setAcctNumber("betty");
        betty.setBalance(BigDecimal.valueOf(1000L));
        betty.setOverdraftLimit(BigDecimal.ZERO);
        betty.setBankCode(bankCode);
        betty.setOpeningTime(current);
        betty.setLastModiTime(current);
        accountDao.insert(betty);
        
        Account lori = new Account();
        lori.setType(AccountType.PERSONAL);
        lori.setAcctName("Lori.Lee");
        lori.setAcctNumber("lori");
        lori.setBalance(BigDecimal.valueOf(1000L));
        lori.setOverdraftLimit(BigDecimal.ZERO);
        lori.setBankCode(bankCode);
        lori.setOpeningTime(current);
        lori.setLastModiTime(current);
        accountDao.insert(lori);
        
        Account abc = new Account();
        abc.setType(AccountType.BANK);
        abc.setAcctName("Agricultural Bank of China");
        abc.setAcctNumber("abc");
        abc.setBalance(BigDecimal.valueOf(100000L));
        abc.setOverdraftLimit(BigDecimal.valueOf(100000L));
        abc.setBankCode(bankCode);
        abc.setOpeningTime(current);
        abc.setLastModiTime(current);
        accountDao.insert(abc);
    }
}
