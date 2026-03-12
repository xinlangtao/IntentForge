package cn.intentforge.session.model;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable command used to append a new message to an existing session.
 *
 * <p>This type is immutable and thread-safe.
 *
 * @param id message identifier
 * @param role message author role
 * @param content message content
 * @param metadata message metadata
 */
public record SessionMessageDraft(
    String id,
    SessionMessageRole role,
    String content,
    Map<String, String> metadata
) {
  /**
   * Creates a validated immutable draft.
   */
  public SessionMessageDraft {
    id = SessionModelSupport.requireText(id, "id");
    role = Objects.requireNonNull(role, "role must not be null");
    content = SessionModelSupport.requireText(content, "content");
    metadata = SessionModelSupport.immutableMetadata(metadata);
  }
}
