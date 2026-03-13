package cn.intentforge.api.agent;


import java.util.Map;

/**
 * HTTP and SSE payload that describes one agent run event.
 *
 * @param runId target run identifier
 * @param sequence ordered event sequence number starting at one
 * @param type run event type name
 * @param status run status name at the time of the event
 * @param message short event message
 * @param metadata event metadata
 * @param createdAt ISO-8601 timestamp string
 */
public record AgentRunEventResponse(
    String runId,
    long sequence,
    String type,
    String status,
    String message,
    Map<String, Object> metadata,
    String createdAt
) {
  /**
   * Creates a validated event response payload.
   */
  public AgentRunEventResponse {
    runId = cn.intentforge.common.util.ValidationSupport.requireText(runId, "runId");
    if (sequence < 1) {
      throw new IllegalArgumentException("sequence must be greater than zero");
    }
    type = cn.intentforge.common.util.ValidationSupport.requireText(type, "type");
    status = cn.intentforge.common.util.ValidationSupport.requireText(status, "status");
    message = cn.intentforge.common.util.ValidationSupport.requireText(message, "message");
    metadata = cn.intentforge.common.util.ValidationSupport.immutableObjectMap(metadata, "metadata");
    createdAt = cn.intentforge.common.util.ValidationSupport.requireText(createdAt, "createdAt");
  }
}
