package net.eric.tpc.bankserver.persist;

import net.eric.tpc.biz.DepositMessage;

public interface DepositMessageDao {
	void insert(DepositMessage message);
}
