package net.eric.tpc.regulator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.base.Optional;

import net.eric.tpc.base.NightWatch;
import net.eric.tpc.base.Pair;
import net.eric.tpc.base.UniFactory;
import net.eric.tpc.biz.Validator;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.net.PeerIoHandler;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.Panticipantor;
import net.eric.tpc.proto.PeerTransactionManager;
import net.eric.tpc.service.BillSaveStrategy;

public class RegulatorServiceFactory extends UniFactory {
    private static final String MY_CLASSIFER = "REGULATOR";

    private ExecutorService pool = Executors.newFixedThreadPool(2);

    public RegulatorServiceFactory() {
        NightWatch.regCloseAction(new Runnable() {
            @Override
            public void run() {
                RegulatorServiceFactory.this.pool.shutdown();
            };
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> Optional<Pair<T, Boolean>> createObject(Class<T> clz, Object classifier) {
        if (PeerIoHandler.class.equals(clz) && this.isRegulator(classifier)) {
            PeerIoHandler handler = new PeerIoHandler();
            handler.setTransManager(getObject(PeerTransactionManager.class, MY_CLASSIFER));
            handler.setTaskPool(pool);
            return Pair.of((T) handler, true);
        }

        if (RegulatorBizStrategy.class.equals(clz)) {
            RegulatorBizStrategy strategy = new RegulatorBizStrategy();
            strategy.setBillSaver(getObject(BillSaveStrategy.class));
            strategy.setBillValidator(getObject(Validator.class, TransferBill.class));
            return Pair.of((T) strategy, true);
        }

        if (PeerTransactionManager.class.equals(clz) && this.isRegulator(classifier)) {
            Panticipantor<TransferBill> panticipantor = new Panticipantor<TransferBill>();
            panticipantor.setBizStrategy(getObject(RegulatorBizStrategy.class));
            panticipantor.setDtLogger(getObject(DtLogger.class));
            return Pair.of((T) panticipantor, true);
        }
        return Optional.absent();
    }

    private boolean isRegulator(Object classifier) {
        return MY_CLASSIFER.equalsIgnoreCase(classifier.toString());
    }

}
