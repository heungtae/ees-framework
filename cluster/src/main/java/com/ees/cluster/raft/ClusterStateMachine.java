package com.ees.cluster.raft;

import com.ees.cluster.lock.DistributedLockService;
import com.ees.cluster.raft.RaftAssignmentService;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.protocol.TermIndex;
import org.apache.ratis.server.storage.RaftStorage;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.statemachine.impl.BaseStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Skeleton state machine that will apply Raft log entries to cluster services.
 */
public class ClusterStateMachine extends BaseStateMachine {

    private static final Logger log = LoggerFactory.getLogger(ClusterStateMachine.class);

    private final RaftAssignmentService assignmentService;
    private final DistributedLockService lockService;
    private final Clock clock;

    public ClusterStateMachine(RaftAssignmentService assignmentService, DistributedLockService lockService) {
        this(assignmentService, lockService, Clock.systemUTC());
    }

    public ClusterStateMachine(RaftAssignmentService assignmentService, DistributedLockService lockService, Clock clock) {
        this.assignmentService = Objects.requireNonNull(assignmentService, "assignmentService must not be null");
        this.lockService = Objects.requireNonNull(lockService, "lockService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void initialize(RaftServer server, RaftGroupId groupId, RaftStorage storage) throws IOException {
        super.initialize(server, groupId, storage);
        log.info("Initialized ClusterStateMachine for group {}", groupId);
    }

    @Override
    public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
        Objects.requireNonNull(trx, "transaction must not be null");
        if (trx.getLogEntry() != null) {
            updateLastAppliedTermIndex(trx.getLogEntry().getTerm(), trx.getLogEntry().getIndex());
        }
        // TODO: decode command JSON and delegate to assignment/lock services.
        byte[] data = trx.getLogEntry() != null && trx.getLogEntry().hasStateMachineLogEntry()
                ? trx.getLogEntry().getStateMachineLogEntry().getLogData().toByteArray()
                : new byte[0];
        Message ack = Message.valueOf(new String(data, StandardCharsets.UTF_8));
        return CompletableFuture.completedFuture(ack);
    }

    @Override
    public long takeSnapshot() throws IOException {
        TermIndex index = getLastAppliedTermIndex();
        // TODO: persist lock and assignment state to snapshot store.
        log.info("Taking snapshot at {}", index);
        return index != null ? index.getIndex() : -1L;
    }

    @Override
    public void reinitialize() throws IOException {
        super.reinitialize();
        log.info("Reinitialized ClusterStateMachine for group {}", getGroupId());
    }
}
