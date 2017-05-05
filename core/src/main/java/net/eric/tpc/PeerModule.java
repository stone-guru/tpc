package net.eric.tpc;

import java.util.List;

import com.google.common.util.concurrent.Service;
import com.google.inject.*;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import net.eric.tpc.net.ChannelFactory;
import net.eric.tpc.net.PeerCommunicator;
import net.eric.tpc.net.RequestHandler;
import net.eric.tpc.net.RequestHandlers;
import net.eric.tpc.net.binary.ObjectCodec;
import net.eric.tpc.net.mina.MinaChannelFactory;
import net.eric.tpc.net.mina.MinaTransService;
import net.eric.tpc.proto.DecisionQuerier;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.Panticipantor;
import net.eric.tpc.proto.PeerBizStrategy;
import net.eric.tpc.proto.PeerTransactionManager;
import net.eric.tpc.service.DtLoggerDbImpl;

public abstract class PeerModule<B> implements Module {

    private final Class<B> entityClass;

    public PeerModule(Class<B> entityClass) {
        this.entityClass = entityClass;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void configure(Binder binder) {

        binder.bind(new EricTypeLiteral<PeerTransactionManager, B>(PeerTransactionManager.class, entityClass))//
                .annotatedWith(Names.named("Peer Trans Manager"))//
                .to(new EricTypeLiteral<Panticipantor, B>(Panticipantor.class, entityClass));

        binder.bind(new EricTypeLiteral<PeerBizStrategy, B>(PeerBizStrategy.class, entityClass))//
                .to(new EricTypeLiteral(getPeerBizStrategyClass(), entityClass));

        binder.bind(DtLogger.class).to(DtLoggerDbImpl.class);
        binder.bind(DecisionQuerier.class).to(PeerCommunicator.class);
        binder.bind(ChannelFactory.class).to(MinaChannelFactory.class);

        binder.bind(Service.class)//
                .to(new EricTypeLiteral(MinaTransService.class, entityClass));
    }

    @Provides
    @Inject
    @Named("RequestHandlers")
    public List<RequestHandler> provideRequestHandlers(@Named("Peer Trans Manager") PeerTransactionManager<B> transManager){
        return RequestHandlers.getBasicHandlers(transManager);
    }

    @Provides
    @Named("Extra Object Codecs")
    public List<ObjectCodec> extraCodecs() {
        return this.getExtraCodecs();
    }

    @Provides
    @Named("Service Port")
    public int port() {
        return this.getPort();
    }

    abstract protected Class<PeerBizStrategy<B>> getPeerBizStrategyClass();

    abstract protected int getPort();

    abstract protected List<ObjectCodec> getExtraCodecs();
}
