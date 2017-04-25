package net.eric.bank.terminal;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import net.eric.bank.biz.BizCode;
import net.eric.bank.biz.Validator;
import net.eric.bank.entity.AccountIdentity;
import net.eric.bank.entity.TransferBill;
import net.eric.bank.service.BillBasicValidator;
import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.proto.Types.ErrorCode;

public class Main {

  
    public static void main(String[] args) {
        System.out.println("Welcome to use ABC transfer terminal T9876");

        Maybe<ExecOptions> options = parseOptions(args);
        if (!options.isRight()) {
            return;
        }

        Maybe<TransferBill> maybe = generateBill(options.getRight());
        if (!maybe.isRight()) {
            System.out.println("Invalid transfer command, " + maybe.getLeft().toString());
            displayUsage();
            return;
        }

        TransferBill bill = maybe.getRight();

        BillSender sender = new BillSender(options.getRight().server);
        try {
            if (!sender.connect()) {
                System.out.println("Unable to connect to " + options.getRight().server);
                return;
            }

            for (int i = 0; i < options.getRight().repeatTimes; i++) {
                bill.setTransSN(genNumber("B" + i));
                bill.setVoucherNumber(genNumber("VC" + i));
                ActionStatus status = sender.sendBillRequest(bill);
                displayResult(status);
            }
        } finally {
            sender.close();
        }
    }

    private static void displayResult(ActionStatus status) {
        if (status.isOK()) {
            System.out.println("Your transfer command has been processed by ABC server succesfully!");
            return;
        }
        String errMsg;
        if (status.getCode() == ErrorCode.PEER_NO_REPLY) {
            errMsg = "ABC server no reponse";
        } else if (status.getCode() == ErrorCode.REFUSE_COMMIT) {
            String[] segs = status.getDescription().split(", *");
            if (segs.length != 3) {
                errMsg = status.getDescription();
            } else {
                errMsg = String.format("%s reply %s %s", segs[0], segs[1], segs[2]);
            }
        } else if (status.getCode() == ErrorCode.PEER_NOT_CONNECTED) {
            errMsg = " ABC server can not connect to " + status.getDescription();
        } else {
            errMsg = status.toString();
        }
        System.out.println("Sorry! Your transfer requirement was reject because of \"" + errMsg + "\".");
    }

    private static void displayUsage() {
        System.out.println(
                "Usage: serverAddress:serverPort accountNumber@bankCode accountNumber@bankCode amount summary");
        System.out.println("bank code must be boc or ccb. ");
        System.out.println("server address and server port is where your ABC server running.");
        System.out.println("amount is a positive integer.");
        System.out.println(
                "summary is any words you want to say. The length of it dont go beyond 64. and it can be ignored.");
        System.out.println("example: 127.0.0.1:10024 james@ccb lori@boc 500 happy new year.");
    }

    private static Maybe<TransferBill> generateBill(ExecOptions options) {

        TransferBill bill = new TransferBill();
        bill.setTransSN(genNumber("B"));
        bill.setLaunchTime(new Date());
        bill.setReceivingBankCode("ABC");
        bill.setPayer(options.payer);
        bill.setReceiver(options.receiver);
        bill.setAmount(options.amount);
        bill.setVoucherNumber(genNumber("VC"));
        bill.setSummary(options.summary);
        // System.out.println(bill);

        Validator<TransferBill> validator = new BillBasicValidator();
        ActionStatus billStatus = validator.check(bill);
        if (!billStatus.isOK()) {
            return Maybe.fail(billStatus);
        }

        return Maybe.success(bill);
    }

    private static String genNumber(String prefix) {
        long t = System.currentTimeMillis() % 1000000000;
        return prefix + String.valueOf(t);
    }

    public static class CmdOptions {
        @Parameter(description = "payer@bank receiver@bank amount summary")
        public List<String> parameters = Lists.newArrayList();

        @Parameter(names = { "-r" }, description = "Repeat this command many times")
        public Integer repeatTimes = 1;

        @Parameter(names = { "-h" }, description = "The ABC server address")
        public String server = "localhost";

        @Parameter(names = { "-p", "--port" }, description = "The ABC server port")
        public Integer port = 10024;
    }

    public static class ExecOptions {
        public InetSocketAddress server;
        public int repeatTimes;
        public List<String> parameters;
        public AccountIdentity payer;
        public AccountIdentity receiver;
        public BigDecimal amount;
        public String summary;
    }

    public static Maybe<ExecOptions> parseOptions(String[] args) {
        CmdOptions options = new CmdOptions();
        ExecOptions execOptions = new ExecOptions();
        JCommander jc = new JCommander(options);
        try {
            jc.parse(args);
        } catch (Exception e) {
            return Maybe.fail(BizCode.COMMAND_SYNTAX_ERROR, e.getMessage());
        }

        if (options.repeatTimes <= 0) {
            return Maybe.fail(BizCode.COMMAND_SYNTAX_ERROR, "Repeat time should be a positive integer");
        }
        if (options.port <= 0) {
            return Maybe.fail(BizCode.COMMAND_SYNTAX_ERROR, "Port should be a positive integer");
        }

        execOptions.server = InetSocketAddress.createUnresolved(options.server, options.port);
        execOptions.repeatTimes = options.repeatTimes;
        execOptions.parameters = options.parameters;

        ActionStatus status = parseTransferArgument(options.parameters, execOptions);
        if (!status.isOK()) {
            return Maybe.fail(status);
        }

        return Maybe.success(execOptions);
    }

    private static ActionStatus parseTransferArgument(List<String> args, ExecOptions options) {
        String command = "";
        if (args != null) {
            StringBuilder builder = new StringBuilder();
            for (String s : args) {
                builder.append(" ").append(s);
            }
            command = builder.toString();
        }

        final String regex = "^\\s*(\\w+)@(\\w+)\\s+(\\w+)@(\\w+)\\s+(-?(\\d+)( *\\. *\\d*)?)(.*)$";
        final Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(command);
        if (!m.find()) {
            return ActionStatus.create(BizCode.COMMAND_SYNTAX_ERROR, "Bad transfer command argument");
        }
        try {
            options.amount = new BigDecimal(m.group(5));
        } catch (Exception e) {
            return ActionStatus.create(BizCode.AMOUNT_FMT_WRONG, "Wrong number format " + m.group(5));
        }

        options.payer = new AccountIdentity(m.group(1), m.group(2));
        options.receiver = new AccountIdentity(m.group(3), m.group(4));
        options.summary = Strings.nullToEmpty(m.group(8));
        
        return ActionStatus.OK;
    }

    private static class BillSender {
        private InetSocketAddress node;
        private SocketConnector connector;
        private IoSession session;

        public BillSender(InetSocketAddress node) {
            this.node = node;
        }

        public boolean connect() {
            connector = new NioSocketConnector();
            connector.setConnectTimeoutMillis(3000);
            DefaultIoFilterChainBuilder filterChain = connector.getFilterChain();
            filterChain.addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));
            connector.getSessionConfig().setUseReadOperation(true);

            connector.connect(new InetSocketAddress(node.getAddress(), node.getPort()));
            ConnectFuture future = connector.connect(new InetSocketAddress(node.getAddress(), node.getPort()));
            future.awaitUninterruptibly();

            if (future.isConnected()) {
                this.session = future.getSession();
                return true;
            }
            return false;
        }

        public ActionStatus sendBillRequest(TransferBill bill) {
//FIXME
//            session.write(new DataPacket(DataPacket.TRANS_BILL, bill));
//            ReadFuture readFuture = session.read();
//            if (!readFuture.awaitUninterruptibly(3000)) {
//                return ActionStatus.PEER_NO_REPLY;
//            }
//            DataPacket dataPacket = (DataPacket) readFuture.getMessage();
//            if (!DataPacket.TRANS_BILL_ANSWER.equals(dataPacket.getCode())) {
//                return ActionStatus.create(ErrorCode.PEER_PRTC_ERROR, "server reply error");
//            }
//            if (DataPacket.YES.equals(dataPacket.getParam1())) {
//                return ActionStatus.OK;
//            }
//            return (ActionStatus) dataPacket.getParam2();
            return null;
        }

        public void close() {
            if (session != null) {
                CloseFuture cf = session.closeNow();
                cf.awaitUninterruptibly();
            }
            if (connector != null) {
                connector.dispose();
            }
        }

    }

}
