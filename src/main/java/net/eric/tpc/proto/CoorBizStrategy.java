package net.eric.tpc.proto;

import java.util.List;
import java.util.concurrent.Future;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.sun.istack.internal.Nullable;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Node;
import net.eric.tpc.base.Pair;
import net.eric.tpc.base.Pair.FieldTag;

/**
 * 事务协调方业务支撑接口.
 * <p>
 * 下列方法的被调用顺序为.
 * <ul>
 * <li>事务开始前 splitTask</li>
 * <li>事务开始后 prepareCommit , 同时也是对voteReq的回应</li>
 * <li>事务提交 commit</li>
 * <li>事务终止 abort</li>
 * </ul>
 * 事务协调器保证如下三种对本接口方法的调用链
 * <ul>
 * <li>splitTask (result.isLeft 不开始事务)</li>
 * <li>splitTask resultOK, parepareCommit resultNo , abort</li>
 * <li>splitTask resultOK, parepareCommit resultYes , commit</li>
 * </ul>
 * 该接口的prepareCommit, commit, abort方法都为异步调用, 其线程调度策略自行负责.
 * 
 * @param <B> 业务对象类型
 */
public interface CoorBizStrategy<B> {

    /**
     * 事务准备。事务协调器在<em>将要</em>开始一个事务时调用此接口。
     * <ul>
     * <li>若业务请求符合规则，可以开始事务返回事务的划分对象. {@link TaskPartition}</li>
     * <li>若事务不能开始, 须返回{@link ActionStatus}来进行错误说明.</li>
     * </ul>
     * 
     * @param xid 本次计划开始的事务编号
     * @param b not null 业务对象,接口自行负责解释其含义
     * @return not null May.right(TaskPartiotion) 或 Maybe.left(ActionStatus)
     */
    Maybe<TaskPartition<B>> splitTask(String xid, B b);

    /**
     * 事务开始后,进行事务准备. 其结果也是vote_req的结果.
     * <p>
     * 若可以提交(VOTE_YES),返回的ActionStatus.code 应为 "OK",建议使用
     * {@link ActionStatus.OK}常量. 否则需返回原因码及其说明.
     * </p>
     * 在本次业务对象传递后,后继操作协调器都不再传递业务对象,若需要实现方自行负责对业务对象的保持与管理.
     * 
     * @param xid 事务编号 not null
     * @param b 需要协调方处理的业务对象,
     *            就是{@link CoorBizStrategy#splitTask}方法中返回的{@link TaskPartition#getCoorTask}
     * @return not null Future of ActionStatus
     */
    Future<ActionStatus> prepareCommit(String xid, B b);

    /**
     * 事务提交
     * 
     * <p>
     * 除事务号外, 协调器还有一些动作需要在提交结束后执行, 这些动作通过一个listener传递进来,
     * 不管成功或失败实现方务必保证调用到该listener的对应方法.
     * </p>
     * FIXME 对协调器来说这样的风险有些大, 后继将修正为自行控制,去掉此listener
     * 
     * @param xid 事务号
     * @param commitListener 动作完成后需要实现方调用的监听器
     * @return not null, Future of null
     */
    Future<Void> commit(String xid, BizActionListener commitListener);

    /**
     * 事务终止. 参数含义同{@link CoorBizStrategy#commit}
     */
    Future<Void> abort(String xid, BizActionListener abortListener);

    /**
     * 事务划分. 表达将要进行的一个事务由那些节点参加, 各个节点需要处理的业务数据.
     * 
     * @param <B> 业务对象类型
     */
    public static class TaskPartition<B> {
        private B coorTask;
        private List<Pair<Node, B>> peerTasks;

        /**
         * 构造方法
         * 
         * <p>
         * 前置体条件 let emptyTask = coorTask is null and peerTasks.isEmpty. <br/>
         * emptyTask is true的事务划分是不允许创建的. 没有事要做请用{@link ActionStatus}表达
         * </p>
         * 任务划分的表达方式为 Pair&lt;节点, 该节点的任务对象&gt;,并且不允许出现对同一个节点有两条任务分配.
         * 
         * @param coorTask 需要协调方处理的业务对象 may null
         * @param peerTasks 其它节点及其对应的需要发送的业务对象 not null,
         */
        public TaskPartition(B coorTask, List<Pair<Node, B>> peerTasks) {
            Preconditions.checkNotNull(peerTasks, "peerTasks");
            Preconditions.checkArgument(coorTask != null || !peerTasks.isEmpty(), "empty tasks is not allowed");

            if (Pair.haskDuplicatedElement(peerTasks, FieldTag.FIRST)) {
                throw new IllegalArgumentException("peerTasks has two task to one peer");
            }

            this.coorTask = coorTask;
            this.peerTasks = ImmutableList.copyOf(peerTasks);
        }

        /**
         * 需要协调器方处理的业务对象
         * 
         * @return may null
         */
        public @Nullable B getCoorTask() {
            return coorTask;
        }

        /**
         * 其它参与方的任务划分
         * 
         * @return 任务划分 not null
         */
        public List<Pair<Node, B>> getPeerTasks() {
            return peerTasks;
        }

        /**
         * 就是PeerTasks的第一分量投影
         * 
         * @return 参与节点列表 not null
         */
        public List<Node> getParticipants() {
            return Pair.projectFirst(peerTasks);
        }
    }
}
