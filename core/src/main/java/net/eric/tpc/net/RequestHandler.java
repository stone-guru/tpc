package net.eric.tpc.net;

import com.google.common.base.Optional;

import net.eric.tpc.net.binary.Message;

public interface RequestHandler {

    short[] getCorrespondingCodes();

    ProcessResult process(TransSession session, Message request);

    class ProcessResult {
        public static ProcessResult NO_RESPONSE_AND_CLOSE = new ProcessResult(true);

        public static ProcessResult errorAndClose(long xid, short round){
            final Message message = new Message(xid, round, CommandCodes.SERVER_ERROR);
            return new ProcessResult(message, true);
        }

        private Message response;
        private boolean closeAfterSend;

        public ProcessResult(boolean closeAfterSend) {
            this(null, closeAfterSend);
        }

        public ProcessResult(Message response, boolean closeAfterSend) {
            this.response = response;
            this.closeAfterSend = closeAfterSend;
        }

        public boolean isCloseAfterSend() {
            return this.closeAfterSend;
        }

        public Optional<Message> getResponse() {
            return Optional.fromNullable(response);
        }
    };

}
