package net.eric.tpc.net;

import com.google.common.base.Optional;

import net.eric.tpc.net.binary.Message;

public interface RequestHandler {

    String getCorrespondingCode();

    ProcessResult process(TransSession session, DataPacket request);

    class ProcessResult {
        public static ProcessResult NO_RESPONSE_AND_CLOSE = new ProcessResult(false);
        
        private DataPacket response;
        private boolean closeAfterSend;

        public ProcessResult(boolean closeAfterSend) {
            this(null, closeAfterSend);
        }

        public ProcessResult(DataPacket response, boolean closeAfterSend) {
            this.response = response;
            this.closeAfterSend = closeAfterSend;
        }

        boolean closeAfterSend() {
            return this.closeAfterSend;
        }

        Optional<DataPacket> getResponse() {
            return Optional.fromNullable(response);
        }
    };

}
