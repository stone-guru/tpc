package net.eric.tpc.bankserver.net;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.net.DataPacketCodec;
import net.eric.tpc.net.HeartBeatRequestHandler;
import net.eric.tpc.net.RequestHandler;
import net.eric.tpc.net.UnknowCommandRequestHandler;
import net.eric.tpc.proto.TransStartRec;
import net.eric.tpc.proto.TransactionManager;
import net.eric.tpc.proto.TransactionState;

public class BankServerHandler extends IoHandlerAdapter {

	private static final String TRANS_SESSION_ITEM = "transactionSession";

	private Map<String, RequestHandler> requestHandlerMap = initRequestHandlers();

	private RequestHandler unknownCommandHandler = new UnknowCommandRequestHandler();

	private TransactionManager transManager;

	private Map<String, RequestHandler> initRequestHandlers() {
		List<RequestHandler> handlers = new ArrayList<RequestHandler>(10);

		handlers.add(new HeartBeatRequestHandler());
		handlers.add(new BeginTransRequestHandler());

		Map<String, RequestHandler> handlerMap = new HashMap<String, RequestHandler>();
		for (RequestHandler h : handlers) {
			handlerMap.put(h.getCorrespondingCode(), h);
		}
		return handlerMap;
	}

	private RequestHandler getRequestHandler(String code) {
		RequestHandler handler = requestHandlerMap.get(code);
		if (handler == null) {
			return this.unknownCommandHandler;
		}
		return handler;
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		cause.printStackTrace();
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		super.sessionCreated(session);
		session.setAttribute(BankServerHandler.TRANS_SESSION_ITEM, new TransactionState());
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		final String s = message.toString().trim();

		System.out.println("Received: " + s);

		DataPacket request = DataPacketCodec.decodeDataPacket(s);
		RequestHandler handler = this.getRequestHandler(request.getCode());
		TransactionState state = (TransactionState) session.getAttribute(BankServerHandler.TRANS_SESSION_ITEM);
		DataPacket response = handler.process(request, state);

		if ("quit".equalsIgnoreCase(s)) {
			session.closeNow();
			return;
		}

		WriteFuture wf = session.write(DataPacketCodec.encodeDataPacket(response));

		final String ss = response.toString();
		wf.addListener(new IoFutureListener() {
			public void operationComplete(IoFuture future) {
				System.out.println(s + ", " + ss + ", Message written");
			}
		});
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
		System.out.println("IDLE " + session.getIdleCount(status));
	}

	public void setTransManager(TransactionManager transManager) {
		this.transManager = transManager;
	}

	class BeginTransRequestHandler implements RequestHandler {
		public String getCorrespondingCode() {
			return DataPacket.BEGIN_TRANS;
		}

		public DataPacket process(DataPacket request, TransactionState state) {
			TransStartRec transNodes = DataPacketCodec.decodeTransStartRec(request.getParam1());
			TransferMessage transMsg = DataPacketCodec.decodeTransferMessage(request.getParam2());
			if (BankServerHandler.this.transManager.beginTrans(transNodes, transMsg, state)) {
				return new DataPacket(DataPacket.BEGIN_TRANS_ANSWER, DataPacket.YES);
			} else {
				return new DataPacket(DataPacket.BAD_COMMNAD, "XXX");
			}
		}
	}
}
