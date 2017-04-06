package net.eric.tpc.proto;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import net.eric.tpc.base.Node;

public class Types {
    public static enum Vote {
        YES("YES"), NO("NO");
           
        public static Vote fromCode(String code){
            for(Vote v : Vote.values()){
                if (v.code().equalsIgnoreCase(code)){
                    return v;
                }
            }
            return null;
        }
        
        private Vote(String s) {
            this.code = s;
        }

        public String code() {
            return this.code;
        }

        private String code;
    }
    
    public static enum Decision {
        COMMIT("COMMIT"), ABORT("ABORT");
        
        public static Decision fromCode(String code){
            for(Decision v : Decision.values()){
                if (v.code().equalsIgnoreCase(code)){
                    return v;
                }
            }
            return null;
        }
        
        private Decision(String s) {
            this.code = s;
        }

        public String code() {
            return this.code;
        }

        private String code;
    }
    
    public static class TransStartRec implements Serializable {
        
        private static final long serialVersionUID = -2699980236712941307L;
        
        private String xid;
        private Node coordinator;
        private List<Node> participants = Collections.emptyList();

        public TransStartRec() {
        }

        public TransStartRec(String xid, Node coordinator, List<Node> participants) {
            super();
            this.xid = xid;
            this.coordinator = coordinator;
            this.participants = participants;
        }

        public String xid(){
            return xid;
        }
        
        public String getXid() {
            return xid;
        }

        public void setXid(String xid) {
            this.xid = xid;
        }

        public Node getCoordinator() {
            return coordinator;
        }

        public void setCoordinator(Node coordinator) {
            this.coordinator = coordinator;
        }

        public List<Node> getParticipants() {
            return participants;
        }

        public void setParticipants(List<Node> participants) {
            this.participants = participants;
        }

        @Override
        public String toString() {
            return "TransStartRec [xid=" + xid + ", coordinator=" + coordinator + ", participants=" + participants + "]";
        }
    }
    
    private Types(){}
}
