package com.ees.cluster.assignment;

import com.ees.cluster.model.Assignment;
import com.ees.cluster.model.TopologyEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Listens to AssignmentService topology events and emits affinity kind changes
 * so downstream components (e.g., workflow engines) can realign their keys.
 */
public class ClusterAffinityKindMonitor {

    private final AssignmentService assignmentService;
    private final Consumer<String> affinityKindConsumer;
    private final AtomicReference<String> lastKind = new AtomicReference<>();
    /**
     * 인스턴스를 생성한다.
     * @param assignmentService 
     * @param affinityKindConsumer 
     */

    public ClusterAffinityKindMonitor(AssignmentService assignmentService, Consumer<String> affinityKindConsumer) {
        this.assignmentService = Objects.requireNonNull(assignmentService, "assignmentService must not be null");
        this.affinityKindConsumer = Objects.requireNonNull(affinityKindConsumer, "affinityKindConsumer must not be null");
        this.assignmentService.topologyEvents(this::onTopologyEvent);
    }
    // onTopologyEvent 동작을 수행한다.

    private void onTopologyEvent(TopologyEvent event) {
        if (event.assignment() != null) {
            emitIfChanged(resolveKindFromAssignment(event.assignment()));
        } else if (event.keyAssignment() != null) {
            emitIfChanged(event.keyAssignment().kind());
        }
    }
    // resolveKindFromAssignment 동작을 수행한다.

    private String resolveKindFromAssignment(Assignment assignment) {
        return assignment.affinities().keySet().stream().findFirst().orElse(null);
    }
    // emitIfChanged 동작을 수행한다.

    private void emitIfChanged(String kind) {
        if (kind == null) {
            return;
        }
        String previous = lastKind.getAndSet(kind);
        if (!kind.equals(previous)) {
            affinityKindConsumer.accept(kind);
        }
    }

    /**
     * Returns the last observed affinity kind, if any.
     */
    public Optional<String> lastKind() {
        return Optional.ofNullable(lastKind.get());
    }
}
