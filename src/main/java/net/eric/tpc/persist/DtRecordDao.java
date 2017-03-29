package net.eric.tpc.persist;

import net.eric.tpc.entity.DtRecord;
import net.eric.tpc.proto.Decision;

public interface DtRecordDao {
    void insert(DtRecord dtRecord);

    void updateVote(DtRecord dtRecord);

    void updateDecision(DtRecord dtRecord);

    Decision selectDecision(String xid);
}
