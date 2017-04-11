package net.eric.tpc.net;

import com.google.common.base.Optional;

import net.eric.tpc.net.binary.Message;

public interface RequestHandler {

    short getCorrespondingCode();

    ProcessResult process(TransSession session, Message request);

    class ProcessResult {
        public static ProcessResult NO_RESPONSE_AND_CLOSE = new ProcessResult(true);
        
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
