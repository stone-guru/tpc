package net.eric.tpc.net;

import com.google.common.base.Optional;

import net.eric.tpc.base.UnImplementedException;
import net.eric.tpc.proto.Types.Decision;

public abstract class DecisionQueryHandler implements RequestHandler {

    @Override
    public String getCorrespondingCode() {
        return DataPacket.DECISION_QUERY;
    }

    @Override
    public ProcessResult process(TransSession session, DataPacket request) {
        String xid = (String) request.getParam1();

        Optional<Decision> decision = this.getDecisionFor(xid);
        
        String param2 = null;
        if (decision.isPresent()) {
            switch (decision.get()) {
            case ABORT:
                param2 = DataPacket.NO;
                break;
            case COMMIT:
                param2 = DataPacket.YES;
                break;
            default:
                throw new UnImplementedException();
            }
        } else {
            param2 = DataPacket.UNKNOWN;
        }

        return new ProcessResult(new DataPacket(DataPacket.DECISION_ANSWER, xid, param2), true);
    }

    protected abstract Optional<Decision> getDecisionFor(String xid);
    
}
