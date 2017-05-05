package net.eric.bank.coor;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import net.eric.bank.common.ServerConfig;
import net.eric.bank.entity.TransferBill;
import net.eric.bank.net.TransferBillCodec;
import net.eric.bank.persist.BankPersistModule;
import net.eric.bank.service.BankServiceModule;
import net.eric.tpc.CoorModule;
import net.eric.tpc.net.binary.ObjectCodec;
import net.eric.tpc.proto.CoorBizStrategy;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Created by bison on 5/3/17.
 */
public class CoorServer {

    public static void main(String[] args) throws Exception {
        ServerConfig config = new ServerConfig(args);
        if (!config.getBankCode().equalsIgnoreCase("CBRC")) {
            System.out.println("BankCode must be CBRC!");
            System.exit(1);
        }
    }

    private Injector initInjector(ServerConfig config, String jdbcUrl) {
        Module persistModule = new BankPersistModule(jdbcUrl);
        Module coorModule = new CoorModule<TransferBill>(TransferBill.class) {
            @Override
            protected List<ObjectCodec> getExtraCodecs() {
                return ImmutableList.of(new TransferBillCodec());
            }

            @Override
            protected InetSocketAddress getCoordinatorAddress() {
                return new InetSocketAddress("localhost", 10024);
            }

            @Override
            protected Class<CoorBizStrategy<TransferBill>> getCoorBizStrategyClass() {
                return (Class<CoorBizStrategy<TransferBill>>) ((Object) AbcBizStrategy.class);
            }
        };

        return Guice.createInjector(persistModule, coorModule, new BankServiceModule());

    }
}

