package net.eric.tpc.persist;

import static net.eric.tpc.base.Pair.asPair;
import static org.testng.Assert.assertEquals;

import java.util.Date;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import net.eric.tpc.base.NightWatch;
import net.eric.tpc.base.UniFactory;
import net.eric.tpc.entity.DtRecord;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.Vote;

public class DtLRecordDaoTest {
    private DtRecordDao dtRecordDao;

    @BeforeSuite
    public static void init() {
        final String dbUrl = "jdbc:h2:/tmp/account_test";
        UniFactory.registerMaybe(new PersisterFactory(dbUrl));
    }

    @AfterSuite
    public static void close() {
        NightWatch.executeCloseActions();
    }

    @BeforeTest
    public void beforeTest() {
        DatabaseInit initor = UniFactory.getObject(DatabaseInit.class);
        initor.dropDtTable();
        initor.createDtTable();

        this.dtRecordDao = UniFactory.getObject(DtRecordDao.class);
        this.dtRecordDao.insert(this.dtRecord());
    }

    @Test
    public void testUpdateVote() {
        DtRecord record = new DtRecord();
        record.setXid(10086L);
        record.setVote(Vote.YES);
        record.setVoteTime(new Date());
        this.dtRecordDao.updateVote(record);
    }

    @Test
    public void testUpdateDecision() {
        DtRecord record = new DtRecord();
        record.setXid(10086L);
        record.setDecision(Decision.ABORT);
        record.setVoteTime(new Date());
        this.dtRecordDao.updateDecision(record);
    }

    @Test
    public void testUpdateFinishTime() {
        this.dtRecordDao.updateFinishTime(asPair(10086L, new Date()));
    }

    @Test
    public void testSelectDecision() {
        this.testUpdateDecision();
        Decision decision = this.dtRecordDao.selectDecision(10086L);
        assertEquals(Decision.ABORT, decision);
    }

    private DtRecord dtRecord(){
        DtRecord record = new DtRecord();
        record.setXid(10086L);
        record.setStartTime(new Date());
        record.setCoordinator("localhost:10024");
        record.setPariticipants("localhost:10021,localhost:10022,localhost:10023");
        record.setBizMessage(new byte[]{1,1,1,1,1,1,1,1});
        record.setStart2PC(true);
        
        return record;
    }
}
