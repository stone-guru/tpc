package net.eric.bank.regulator;

import com.google.common.base.Optional;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import net.eric.bank.biz.Validator;
import net.eric.bank.bod.AccountLocker;
import net.eric.bank.common.PeerServer;
import net.eric.bank.common.ServerConfig;
import net.eric.bank.entity.TransferBill;
import net.eric.bank.service.BillBasicValidator;
import net.eric.tpc.proto.PeerBizStrategy;

public class RegulatorServer {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        ServerConfig config = new ServerConfig(args);
        if (!config.getBankCode().equalsIgnoreCase("CBRC")) {
            System.out.println("BankCode must be CBRC");
            return;
        }
        Class<?> c = RegulatorBizStrategy.class;

        Module m3 = (Binder binder) -> {
            binder.bind(new TypeLiteral<Validator<TransferBill>>() {
            }).to(BillBasicValidator.class);
            binder.bind(AccountLocker.class);
        };

        PeerServer.runServer(config, (Class<PeerBizStrategy<TransferBill>>) c, m3);
    }
}
