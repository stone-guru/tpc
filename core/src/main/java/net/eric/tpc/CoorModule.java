package net.eric.tpc;

import java.net.InetSocketAddress;
import java.util.List;

import com.google.inject.Binder;
import com.google.inject.EricTypeLiteral;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import net.eric.tpc.net.CoorCommunicatorFactory;
import net.eric.tpc.net.binary.ObjectCodec;
import net.eric.tpc.proto.CommunicatorFactory;
import net.eric.tpc.proto.CoorBizStrategy;
import net.eric.tpc.proto.Coordinator;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.KeyGenerator;
import net.eric.tpc.proto.TransactionManager;
import net.eric.tpc.service.DefaultKeyGenerator;
import net.eric.tpc.service.DtLoggerDbImpl;
import net.eric.tpc.service.KeyPersister;
import net.eric.tpc.service.KeyPersisterDbImpl;

public abstract class CoorModule<B> implements Module {

    private final Class<B> entityClass;

    public CoorModule(Class<B> entityClass) {
        this.entityClass = entityClass;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void configure(Binder binder) {
        binder.bind(new EricTypeLiteral<TransactionManager, B>(TransactionManager.class, entityClass))//
                .to(new EricTypeLiteral<Coordinator, B>(Coordinator.class, entityClass));
        binder.bind(new EricTypeLiteral<Coordinator, B>(Coordinator.class, entityClass)).in(Scopes.SINGLETON);

        binder.bind(KeyPersister.class).to(KeyPersisterDbImpl.class);
        binder.bind(KeyGenerator.class).to(DefaultKeyGenerator.class);

        binder.bind(DtLogger.class).to(DtLoggerDbImpl.class);

        binder.bind(new EricTypeLiteral<CoorBizStrategy, B>(CoorBizStrategy.class, entityClass))//
                .annotatedWith(Names.named("CoorBizStrategy"))//
                .to(new EricTypeLiteral(getCoorBizStrategyClass(), entityClass));

        binder.bind(CommunicatorFactory.class).to(CoorCommunicatorFactory.class);
        binder.bind(CoorCommunicatorFactory.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Named("Coordinator InetSocketAddress")
    public InetSocketAddress coordinatorAddress() {
        return this.getCoordinatorAddress();
    }

    @Provides
    @Named("Extra Object Codecs")
    public List<ObjectCodec> extraCodecs() {
        return this.getExtraCodecs();
    }

    abstract protected List<ObjectCodec> getExtraCodecs();

    abstract protected InetSocketAddress getCoordinatorAddress();

    abstract protected Class<CoorBizStrategy<B>> getCoorBizStrategyClass();
}
