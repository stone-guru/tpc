package net.eric.tpc.bankserver.persist;

import net.eric.tpc.biz.TransferMessage;

public interface TransferMessageDao {
	void insert(TransferMessage message);
}
