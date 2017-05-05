package net.eric.bank.common;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Service;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import net.eric.bank.biz.AccountRepository;
import net.eric.bank.entity.Account;
import net.eric.bank.entity.TransferBill;
import net.eric.bank.net.TransferBillCodec;
import net.eric.bank.persist.BankPersistModule;
import net.eric.bank.util.Util;
import net.eric.tpc.PeerModule;
import net.eric.tpc.base.NightWatch;
import net.eric.tpc.net.binary.ObjectCodec;
import net.eric.tpc.proto.PeerBizStrategy;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class PeerServer {
    public static void runServer(ServerConfig config, Class<PeerBizStrategy<TransferBill>> bizStrategy,
            Module... extraModules) {
        final PeerServer server = new PeerServer(config, bizStrategy, extraModules);
        final boolean started = server.startService();
        if (started) {
            server.installSignalHandler();
            server.waitTerminate();
            
            NightWatch.executeCloseActions();
        }
    }

    private Service service;
    private ServerConfig config;

    public PeerServer(ServerConfig config, Class<PeerBizStrategy<TransferBill>> c, Module... extraModules) {
        Injector injector = this.initInjector(config, c, extraModules);
        this.service = injector.getInstance(Service.class);
        this.config = config;
    }

    public boolean startService() {
        this.service.startAsync();
        try {
            this.service.awaitRunning(5000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void installSignalHandler() {
        SignalHandler handler = new SignalHandler() {
            @Override
            public void handle(Signal arg0) {
                service.stopAsync();
            }
        };

        Signal.handle(new Signal("TERM"), handler);
        Signal.handle(new Signal("INT"), handler);
    }

    public void waitTerminate() {
        this.service.awaitTerminated();
    }

    private Injector initInjector(ServerConfig config, Class<PeerBizStrategy<TransferBill>> c, Module... extraModules) {
        Module m1 = new BankPersistModule(config.getDbUrl());
        Module m2 = new PeerModule<TransferBill>(TransferBill.class) {
            @Override
            public Class<PeerBizStrategy<TransferBill>> getPeerBizStrategyClass() {
                return c;
            }

            @Override
            public int getPort() {
                return config.getPort();
            }

            @Override
            public List<ObjectCodec> getExtraCodecs() {
                return ImmutableList.of(new TransferBillCodec());
            }
        };

        final int mn  = extraModules == null? 2 : 2 + extraModules.length;
        Module[] modules = new Module[mn];
        modules[0] = m1;
        modules[1] = m2;
        for(int i = 2; i < mn ; i++)
            modules[i] = extraModules[i - 2];
        Injector in = Guice.createInjector(modules);
        return in;
    }

    protected String getSplashText(String bankCode) {
        if (bankCode.equalsIgnoreCase("CCB")) {
            return "          /$$$$$$   /$$$$$$  /$$$$$$$ \n"//
                    + "         /$$__  $$ /$$__  $$| $$__  $$\n"//
                    + "        | $$  \\__/| $$  \\__/| $$  \\ $$\n"//
                    + "        | $$      | $$      | $$$$$$$ \n"//
                    + "        | $$      | $$      | $$__  $$\n"//
                    + "        | $$    $$| $$    $$| $$  \\ $$\n"//
                    + "        |  $$$$$$/|  $$$$$$/| $$$$$$$/\n"//
                    + "        \\______/  \\______/ |_______/  ";

        }
        if (bankCode.equalsIgnoreCase("BOC")) {
            return "         /$$$$$$$   /$$$$$$   /$$$$$$ \n"//
                    + "        | $$__  $$ /$$__  $$ /$$__  $$\n"//
                    + "        | $$  \\ $$| $$  \\ $$| $$  \\__/\n"//
                    + "        | $$$$$$$ | $$  | $$| $$      \n"//
                    + "        | $$__  $$| $$  | $$| $$      \n"//
                    + "        | $$  \\ $$| $$  | $$| $$    $$\n"//
                    + "        | $$$$$$$/|  $$$$$$/|  $$$$$$/\n"//
                    + "        |_______/  \\______/  \\______/ ";
        }
        return null;
    }

    private void displayAllAccount() {
        AccountRepository accountRepo = null;// FIXME
                                             // UniFactory.getObject(AccountRepositoryImpl.class);
        List<Account> accounts = accountRepo.getAllAccount();
        Util.displayAccounts(accounts);
    }

}
