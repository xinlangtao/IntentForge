package cn.intentforge.agent.core;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * One message captured during an event-driven agent run.
 *
 * @param id stable message identifier
 * @param role message author role
 * @param content message content
 * @param createdAt message creation timestamp
 * @param metadata optional message metadata
 */
public record AgentRunMessage(
    String id,
    AgentRunMessageRole role,
    String content,
    Instant createdAt,
    Map<String, String> metadata
) {
  /**
   * Creates a validated run message.
   */
  public AgentRunMessage {
    id = AgentModelSupport.requireText(id, "id");
    role = Objects.requireNonNull(role, "role must not be null");
    content = AgentModelSupport.requireText(content, "content");
    createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    metadata = AgentModelSupport.immutableMetadata(metadata);
  }
}
