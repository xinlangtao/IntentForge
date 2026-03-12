package cn.intentforge.session.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable message snapshot stored inside a session.
 *
 * <p>This type is immutable and thread-safe.
 *
 * @param id message identifier
 * @param role message author role
 * @param content message content
 * @param metadata message metadata
 * @param createdAt message creation timestamp
 */
public record SessionMessage(
    String id,
    SessionMessageRole role,
    String content,
    Map<String, String> metadata,
    Instant createdAt
) {
  /**
   * Creates a validated immutable message snapshot.
   */
  public SessionMessage {
    id = SessionModelSupport.requireText(id, "id");
    role = Objects.requireNonNull(role, "role must not be null");
    content = SessionModelSupport.requireText(content, "content");
    metadata = SessionModelSupport.immutableMetadata(metadata);
    createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
  }
}
