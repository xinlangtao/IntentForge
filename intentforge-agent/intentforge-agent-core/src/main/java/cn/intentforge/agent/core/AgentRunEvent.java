package cn.intentforge.agent.core;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * One transport-agnostic event emitted by the run lifecycle.
 *
 * @param runId target run identifier
 * @param sequence ordered event sequence number starting at one
 * @param type event type
 * @param status run status at the time of emission
 * @param message short event message
 * @param metadata optional event metadata
 * @param createdAt event creation timestamp
 */
public record AgentRunEvent(
    String runId,
    long sequence,
    AgentRunEventType type,
    AgentRunStatus status,
    String message,
    Map<String, Object> metadata,
    Instant createdAt
) {
  /**
   * Creates a validated event.
   */
  public AgentRunEvent {
    runId = AgentModelSupport.requireText(runId, "runId");
    if (sequence < 1) {
      throw new IllegalArgumentException("sequence must be greater than zero");
    }
    type = Objects.requireNonNull(type, "type must not be null");
    status = Objects.requireNonNull(status, "status must not be null");
    message = AgentModelSupport.requireText(message, "message");
    metadata = AgentModelSupport.immutableObjectMap(metadata);
    createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
  }
}
