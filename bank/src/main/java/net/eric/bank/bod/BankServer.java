package net.eric.bank.bod;

import com.google.common.base.Optional;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import net.eric.bank.biz.Validator;
import net.eric.bank.common.PeerServer;
import net.eric.bank.common.ServerConfig;
import net.eric.bank.entity.TransferBill;
import net.eric.bank.service.BillBasicValidator;
import net.eric.tpc.proto.PeerBizStrategy;

public class BankServer {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        ServerConfig config = new ServerConfig(args);
        if (!config.getBankCode().equalsIgnoreCase("BOC") && !config.getBankCode().equalsIgnoreCase("CCB")) {
            System.out.println("BankCode must be BOC or CCB");
            return;
        }
        Class<?> c = AccountRepositoryImpl.class;
        
        Module m3 = new Module() {
            @Override
            public void configure(Binder binder) {
                binder.bind(new TypeLiteral<Validator<TransferBill>>() {
                }).to(BillBasicValidator.class);
                
                binder.bind(AccountLocker.class);
            }
        };
        
        PeerServer.runServer(config, (Class<PeerBizStrategy<TransferBill>>) c, Optional.of(m3));
    }
}
