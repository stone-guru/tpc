package net.eric.tpc.bankserver.persist;

import java.io.Reader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import net.eric.tpc.bankserver.entity.Account;
import net.eric.tpc.biz.DepositMessage;
import net.eric.tpc.biz.MessageType;

public class H2DbTest {
	private static SqlSession session;

	public static void main(String[] args) throws Exception {
		Reader reader = Resources.getResourceAsReader("net/eric/tpc/bankserver/persist/mapper/bankserver-config.xml");
		Properties properties = new Properties();
		properties.put("url", "jdbc:h2:tcp://localhost:9100/bank");

		SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader, properties);
		session = sqlSessionFactory.openSession();
		AccountDao accountDao = session.getMapper(AccountDao.class);
		insertDepositMessage();
		//accountDao.createAccountTable();
		
		//initAccounts(accountDao, "BOC");
		
		//accountDao.modifyBalance(asPair("mike", BigDecimal.valueOf(-100)));
		
//		DatabaseInit databaseInit = session.getMapper(DatabaseInit.class);
//		databaseInit.createAccountTable();
//		databaseInit.createDepositMessageTable();

		
		session.commit();
		session.close();
	}

	public static void insertDepositMessage() {
		DepositMessageDao dmDao = session.getMapper(DepositMessageDao.class);
		DepositMessage msg = new DepositMessage();
		msg.setVersion("1.1");
		msg.setMessageNumber("2193983");
		msg.setLaunchTime(new Date());
		msg.setSender("BOC Server");
		msg.setReceiver("KOT Server");
		msg.setType(MessageType.NORMAL);
		
		msg.setTransSN("982872393");
		msg.setAccountNumber("mike");
		msg.setBankCode("BOC");
		msg.setAmount(BigDecimal.valueOf(200));
		msg.setDocNumber("BIK09283-33843");
		
		dmDao.insert(msg);
		
	}
	public static void initAccounts(AccountDao accountDao, String bankCode) {
		List<String> names = Arrays.asList("mike", "tom", "rose", "kate");
		Date now = new Date();
		BigDecimal balance = BigDecimal.valueOf(1000L);

		for (String name : names) {
			Account account = new Account();
			account.setAcctName(name);
			account.setAcctNumber(name);
			account.setBalance(balance);
			account.setBankCode(bankCode);
			account.setOpeningTime(now);
			account.setLastModiTime(now);

			accountDao.insert(account);
			System.out.println("Insert account " + account.getAcctName());
		}
	}
}
