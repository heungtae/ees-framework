package com.ees.cluster.raft;

import com.ees.cluster.lock.DistributedLockService;
import com.ees.cluster.model.Assignment;
import com.ees.cluster.raft.RaftAssignmentService;
import com.ees.cluster.raft.command.AssignKeyCommand;
import com.ees.cluster.raft.command.AssignPartitionCommand;
import com.ees.cluster.raft.command.CommandType;
import com.ees.cluster.raft.command.LockCommand;
import com.ees.cluster.raft.command.RaftCommandCodec;
import com.ees.cluster.raft.command.RaftCommandEnvelope;
import com.ees.cluster.raft.command.ReleaseLockCommand;
import com.ees.cluster.raft.command.RevokePartitionCommand;
import com.ees.cluster.raft.command.UnassignKeyCommand;
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
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
        byte[] data = extractLogData(trx);
        if (trx.getLogEntry() != null) {
            updateLastAppliedTermIndex(trx.getLogEntry().getTerm(), trx.getLogEntry().getIndex());
        }
        if (data.length == 0) {
            return CompletableFuture.completedFuture(Message.valueOf("empty"));
        }
        try {
            RaftCommandEnvelope envelope = RaftCommandCodec.deserialize(data);
            return handleCommand(envelope)
                    .thenApply(ignored -> Message.valueOf(envelope.type().name()))
                    .toCompletableFuture();
        } catch (Exception e) {
            log.error("Failed to apply raft command", e);
            return CompletableFuture.completedFuture(Message.valueOf("error:" + e.getClass().getSimpleName()));
        }
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

    private byte[] extractLogData(TransactionContext trx) {
        if (trx.getLogEntry() == null || !trx.getLogEntry().hasStateMachineLogEntry()) {
            return new byte[0];
        }
        return trx.getLogEntry().getStateMachineLogEntry().getLogData().toByteArray();
    }

    private CompletionStage<Void> handleCommand(RaftCommandEnvelope envelope) {
        return switch (envelope.type()) {
            case LOCK_ACQUIRE -> {
                LockCommand cmd = (LockCommand) envelope.command();
                yield lockService.tryAcquire(cmd.name(), cmd.ownerNodeId(), Duration.ofMillis(cmd.leaseMillis()), cmd.metadata())
                        .then()
                        .toFuture();
            }
            case LOCK_RELEASE -> {
                ReleaseLockCommand cmd = (ReleaseLockCommand) envelope.command();
                yield lockService.release(cmd.name(), cmd.ownerNodeId()).then().toFuture();
            }
            case ASSIGN_PARTITION -> {
                AssignPartitionCommand cmd = (AssignPartitionCommand) envelope.command();
                Assignment assignment = new Assignment(cmd.groupId(), cmd.partition(), cmd.ownerNodeId(),
                        cmd.equipmentIds(), cmd.workflowHandoff(), 0L, clock.instant());
                yield assignmentService.applyAssignments(cmd.groupId(), java.util.List.of(assignment)).toFuture();
            }
            case REVOKE_PARTITION -> {
                RevokePartitionCommand cmd = (RevokePartitionCommand) envelope.command();
                yield assignmentService.revokeAssignments(cmd.groupId(), java.util.List.of(cmd.partition()), cmd.reason())
                        .toFuture();
            }
            case ASSIGN_KEY -> {
                AssignKeyCommand cmd = (AssignKeyCommand) envelope.command();
                yield assignmentService.assignKey(cmd.groupId(), cmd.partition(), cmd.key(), cmd.appId(), cmd.source())
                        .then()
                        .toFuture();
            }
            case UNASSIGN_KEY -> {
                UnassignKeyCommand cmd = (UnassignKeyCommand) envelope.command();
                yield assignmentService.unassignKey(cmd.groupId(), cmd.partition(), cmd.key())
                        .then()
                        .toFuture();
            }
        };
    }
}
