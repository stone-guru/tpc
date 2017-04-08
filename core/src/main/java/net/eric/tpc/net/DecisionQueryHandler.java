package net.eric.tpc.net;

import com.google.common.base.Optional;

import net.eric.tpc.base.UnImplementedException;
import net.eric.tpc.net.binary.Message;
import net.eric.tpc.proto.Types.Decision;

public abstract class DecisionQueryHandler implements RequestHandler {

    @Override
    public short getCorrespondingCode() {
        return CommandCodes.DECISION_QUERY;
    }

    @Override
    public ProcessResult process(TransSession session, Message request) {
        long xid = request.getXid();

        Optional<Decision> decision = this.getDecisionFor(xid);

        short answer = 0;
        if (decision.isPresent()) {
            switch (decision.get()) {
            case ABORT:
                answer = CommandCodes.YES;
                break;
            case COMMIT:
                answer = CommandCodes.NO;
                break;
            default:
                throw new UnImplementedException();
            }
        } else {
            answer = CommandCodes.UNKNOWN;
        }

        return new ProcessResult(Message.fromRequest(request, CommandCodes.DECISION_ANSWER, answer), true);
    }

    protected abstract Optional<Decision> getDecisionFor(long xid);

}
