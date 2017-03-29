package net.eric.tpc.proto;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import net.eric.tpc.common.ActionStatus;
import net.eric.tpc.common.Maybe;
import net.eric.tpc.common.Node;
import net.eric.tpc.common.Pair;

public interface CoorBizStrategy<B> {

    Future<ActionStatus> prepareCommit(String xid, B b);
    
    Maybe<TaskPartition<B>> splitTask(String xid, B b);

    Future<Void> commit(String x, BizActionListener commitListener);
    
    Future<Void> abort(String x, BizActionListener abortListener);
    

    public static class TaskPartition<B> {
        private B coorTask;
        private List<Pair<Node, B>> peerTasks;

        public TaskPartition(B coorTask, List<Pair<Node, B>> peerTasks) {
            this.coorTask = coorTask;
            this.peerTasks = peerTasks;
        }

        public B getCoorTask() {
            return coorTask;
        }

        public List<Pair<Node, B>> getPeerTasks() {
            return peerTasks;
        }
        
        public List<Node> getParticipants() {
            List<Pair<Node, B>> tasks = this.getPeerTasks();
            List<Node> nodes = new ArrayList<Node>(tasks.size());
            for (Pair<Node, B> task : tasks) {
                nodes.add(task.fst());
            }
            return nodes;
        }
    }
}
