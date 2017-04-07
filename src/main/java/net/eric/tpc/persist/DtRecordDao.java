package net.eric.tpc.persist;

import java.util.Date;

import net.eric.tpc.base.Pair;
import net.eric.tpc.entity.DtRecord;
import net.eric.tpc.proto.Types.Decision;

public interface DtRecordDao {
    void insert(DtRecord dtRecord);

    void updateVote(DtRecord dtRecord);

    void updateDecision(DtRecord dtRecord);

    void updateFinishTime(Pair<Long, Date> param);
    
    Decision selectDecision(long xid);
}
