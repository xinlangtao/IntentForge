package cn.intentforge.agent.core;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of one event-driven run at a point in time.
 *
 * @param runId stable run identifier
 * @param task task associated with the run
 * @param status current run status
 * @param contextPack resolved runtime context
 * @param route selected route
 * @param state accumulated execution state
 * @param events ordered emitted events
 * @param awaitingReason optional reason shown while awaiting user input
 * @param nextStepIndex zero-based index of the next route step to execute
 * @param availableNextActions user-selectable next actions visible at the current checkpoint
 * @param createdAt run creation timestamp
 * @param updatedAt latest run update timestamp
 */
public record AgentRunSnapshot(
    String runId,
    AgentTask task,
    AgentRunStatus status,
    ContextPack contextPack,
    AgentRoute route,
    AgentExecutionState state,
    List<AgentRunEvent> events,
    String awaitingReason,
    int nextStepIndex,
    List<AgentRunAvailableAction> availableNextActions,
    Instant createdAt,
    Instant updatedAt
) {
  /**
   * Creates a validated snapshot.
   */
  public AgentRunSnapshot {
    runId = cn.intentforge.common.util.ValidationSupport.requireText(runId, "runId");
    task = Objects.requireNonNull(task, "task must not be null");
    status = Objects.requireNonNull(status, "status must not be null");
    contextPack = Objects.requireNonNull(contextPack, "contextPack must not be null");
    route = Objects.requireNonNull(route, "route must not be null");
    state = Objects.requireNonNull(state, "state must not be null");
    events = AgentModelSupport.immutableList(events, "events");
    awaitingReason = cn.intentforge.common.util.ValidationSupport.normalize(awaitingReason);
    if (nextStepIndex < 0) {
      throw new IllegalArgumentException("nextStepIndex must not be negative");
    }
    availableNextActions = AgentModelSupport.immutableList(availableNextActions, "availableNextActions");
    createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    if (updatedAt.isBefore(createdAt)) {
      throw new IllegalArgumentException("updatedAt must not be earlier than createdAt");
    }
  }
}
