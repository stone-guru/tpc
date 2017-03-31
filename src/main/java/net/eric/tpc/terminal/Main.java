package net.eric.tpc.terminal;

import static net.eric.tpc.base.Pair.asPair;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Node;
import net.eric.tpc.base.Pair;
import net.eric.tpc.biz.BizCode;
import net.eric.tpc.entity.AccountIdentity;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.net.DataPacket;

public class Main {

    public static void main(String[] args) {
        System.out.println("Welcome to use ABC transfer terminal T9876");

        Maybe<Pair<Node, TransferBill>> maybe = analyzeCommand(args);
        if (!maybe.isRight()) {
            System.out.println("Invalid transfer command.");
            displayUsage();
        }

        Node node = maybe.getRight().fst();
        TransferBill bill = maybe.getRight().snd();

        BillSender sender = new BillSender(node);
        try {
            if (!sender.connect()) {
                System.out.println("Unable to connect to " + node);
                return;
            }

            ActionStatus status = sender.sendBillRequest(bill);
            displayResult(status);
        } finally {
            sender.close();
        }
    }

    private static void displayResult(ActionStatus status) {
        if (status.isOK()) {
            System.out.println("Your transfer command has been processed by ABC server succesfully!");
        } else {
            String errMsg;
            if (status.getCode().equals(ActionStatus.PEER_NO_REPLY)) {
                errMsg = "ABC server no reponse";
            } else {
                errMsg = "ABC server reply " + status.toString();
            }
            System.out.println("Your transfer command is not processed. The reason is " + errMsg);
        }
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

    private static Maybe<Pair<Node, TransferBill>> analyzeCommand(String[] args) {
        String command = "";
        if (args != null) {
            StringBuilder builder = new StringBuilder();
            for (String s : args) {
                builder.append(" ").append(s);
            }
            command = builder.toString();
        }

        final String regex = "^\\s*((\\w+)(\\.\\w+)*):(\\d+)\\s+(\\w+)@(\\w+)\\s+(\\w+)@(\\w+)\\s+(\\d+)(.*)$";
        final Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(command);
        if (!m.find()) {
            return Maybe.fail(BizCode.COMMAND_SYNTAX_ERROR, "");
        }
        AccountIdentity account1 = new AccountIdentity(m.group(5), m.group(6));
        AccountIdentity account2 = new AccountIdentity(m.group(7), m.group(8));
        int amount = Integer.parseInt(m.group(9));
        String summary = m.group(10);
        Node node = new Node(m.group(1), Integer.parseInt(m.group(4)));

        TransferBill bill = new TransferBill();
        bill.setTransSN(genNumber("B"));
        bill.setLaunchTime(new Date());
        bill.setReceivingBankCode("ABC");
        bill.setAccount(account1);
        bill.setOppositeAccount(account2);
        bill.setAmount(BigDecimal.valueOf(amount));
        bill.setVoucherNumber(genNumber("VC"));
        bill.setSummary(summary == null ? "" : summary.trim());
        System.out.println(bill);
        return Maybe.success(asPair(node, bill));
    }

    private static String genNumber(String prefix) {
        long t = System.currentTimeMillis() % 1000000000;
        return prefix + String.valueOf(t);
    }

    private static class BillSender {
        private Node node;
        private SocketConnector connector;
        private IoSession session;

        public BillSender(Node node) {
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

            session.write(new DataPacket(DataPacket.TRANS_BILL, bill));
            ReadFuture readFuture = session.read();
            if (!readFuture.awaitUninterruptibly(3000)) {
                return ActionStatus.PEER_NO_REPLY;
            }
            DataPacket dataPacket = (DataPacket) readFuture.getMessage();
            if (!DataPacket.TRANS_BILL_ANSWER.equals(dataPacket.getCode())) {
                return ActionStatus.create(BizCode.PEER_PRTC_ERROR, "server reply error");
            }
            if (DataPacket.YES.equals(dataPacket.getParam1())) {
                return ActionStatus.OK;
            }
            return (ActionStatus) dataPacket.getParam2();
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
