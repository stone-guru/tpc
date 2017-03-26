package net.eric.tpc.coor.stub;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.google.common.base.Optional;

import net.eric.tpc.common.Pair;
import net.eric.tpc.net.CommunicationRound;
import net.eric.tpc.net.CommunicationRound.RoundType;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.net.DataPacketCodec;
import net.eric.tpc.proto.Node;

public class MinaChannel {
	private SocketConnector connector;
	private IoSession session;
	private Node node;
	private AtomicReference<CommunicationRound> roundRef;

	public MinaChannel(Node node, AtomicReference<CommunicationRound> roundRef) {
		this.node = node;
		this.roundRef = roundRef;
	}

	public boolean connect() throws Exception {
		this.connector = new NioSocketConnector();
		this.connector.setConnectTimeoutMillis(3000);
		DefaultIoFilterChainBuilder filterChain = connector.getFilterChain();
		filterChain.addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));
		// connector.getSessionConfig().setUseReadOperation(true);
		this.connector.setHandler(new SocketHandler());
		ConnectFuture future = connector.connect(new InetSocketAddress(node.getAddress(), node.getPort()));
		System.out.println("Ask connect");
		future.awaitUninterruptibly();
		this.session = future.getSession();
		System.out.println("Connect ok");
		session.setAttribute("ROUND_REF", this.roundRef);
		session.setAttribute("PEER", this.node);
		return true;
	}

	public IoSession session() {
		return this.session;
	}

	public Node node() {
		return this.node;
	}

	public void sendMessage(Object request){
		this.sendMessage(request, false);
	}
	
	public void sendMessage(Object request, boolean sendOnly) {
		final WriteFuture writeFuture = session.write(request);
		writeFuture.awaitUninterruptibly();
		if (!writeFuture.isWritten()) {
			if (writeFuture.getException() != null) {
				CommunicationRound round = roundRef.get();
				round.regFailure(this.node, "ERROR_SEND", writeFuture.getException().getMessage());
			}
		}else if (sendOnly){
			CommunicationRound round = roundRef.get();
			round.regMessage(node, true);
		}
	}

	public DataPacket request(DataPacket request) throws Exception {

		final String s = DataPacketCodec.encodeDataPacket(request);

		final WriteFuture writeFuture = session.write(s);
		writeFuture.awaitUninterruptibly();
		if (writeFuture.getException() != null) {
			return new DataPacket("ERROR", "Write error: " + writeFuture.getException().getMessage());
		}

		final ReadFuture readFuture = session.read();
		readFuture.awaitUninterruptibly();

		if (readFuture.getException() != null) {
			return new DataPacket("ERROR", "Read error: " + readFuture.getException().getMessage());
		}

		final String responseString = readFuture.getMessage().toString();
		return DataPacketCodec.decodeDataPacket(responseString);
	}

	public boolean close() throws Exception {
		CloseFuture future = session.getCloseFuture();
		future.awaitUninterruptibly(1000);
		connector.dispose();
		return true;
	}

	private static class SocketHandler extends IoHandlerAdapter {

		@Override
		public void sessionClosed(IoSession session) throws Exception {
			Optional<Pair<CommunicationRound, Node>> opt = this.currentRound(session);
			if (opt.isPresent()) {
				CommunicationRound round = opt.get().fst();
				Node node = opt.get().snd();
				round.connectionClosed(node);
			}
			super.sessionClosed(session);
		}

		@Override
		public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
			// TODO Auto-generated method stub
			super.sessionIdle(session, status);
		}

		@Override
		public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
			super.exceptionCaught(session, cause);
		}

		@Override
		public void messageReceived(IoSession session, Object message) throws Exception {
			System.out.println("Got message from " + session.getServiceAddress().toString() + ", " + message);
			Optional<Pair<CommunicationRound, Node>> opt = this.currentRound(session);
			if (opt.isPresent()) {
				CommunicationRound round = opt.get().fst();
				Node node = opt.get().snd();
				round.regMessage(node, message);
			}
		}

		private Optional<Pair<CommunicationRound, Node>> currentRound(IoSession session) {
			@SuppressWarnings("unchecked")
			AtomicReference<CommunicationRound> roundRef = (AtomicReference<CommunicationRound>) session
					.getAttribute("ROUND_REF");
			if (roundRef.get() == null) {
				System.out.println("No communication round object found, abandom result");
				return Optional.absent();
			}

			CommunicationRound round = roundRef.get();
			if (!round.isWithinRound()) {
				System.out.println("Not in communication round, abandom result");
			}
			if (round.roundType() != RoundType.DOUBLE_SIDE) {
				System.out.println("This round does not require answer, abandom result");
				return Optional.absent();
			}

			Node node = (Node) session.getAttribute("PEER");

			if (node == null) {
				System.out.println("No PEER node set");
				return Optional.absent();
			}
			return Optional.of(Pair.asPair(round, node));
		}
	}
}