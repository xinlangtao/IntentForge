package cn.intentforge.session.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable snapshot of one managed session.
 *
 * <p>This type is immutable and thread-safe.
 *
 * @param id session identifier
 * @param title optional human readable title
 * @param spaceId optional owning space identifier
 * @param status session lifecycle status
 * @param messages ordered message history
 * @param metadata session metadata
 * @param createdAt session creation timestamp
 * @param updatedAt latest session update timestamp
 */
public record Session(
    String id,
    String title,
    String spaceId,
    SessionStatus status,
    List<SessionMessage> messages,
    Map<String, String> metadata,
    Instant createdAt,
    Instant updatedAt
) {
  /**
   * Creates a validated immutable session snapshot.
   */
  public Session {
    id = SessionModelSupport.requireText(id, "id");
    title = SessionModelSupport.normalize(title);
    spaceId = SessionModelSupport.normalize(spaceId);
    status = Objects.requireNonNullElse(status, SessionStatus.ACTIVE);
    messages = SessionModelSupport.immutableMessages(messages);
    metadata = SessionModelSupport.immutableMetadata(metadata);
    createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    if (updatedAt.isBefore(createdAt)) {
      throw new IllegalArgumentException("updatedAt must not be earlier than createdAt");
    }
  }

  /**
   * Evaluates whether this session matches the supplied query.
   *
   * @param query query filter; {@code null} means match all
   * @return {@code true} when the session matches
   */
  public boolean matches(SessionQuery query) {
    if (query == null) {
      return true;
    }
    if (query.spaceId() != null && !query.spaceId().equals(spaceId)) {
      return false;
    }
    if (query.status() != null && query.status() != status) {
      return false;
    }
    if (query.keyword() == null) {
      return true;
    }
    String keyword = query.keyword();
    if (title != null && title.toLowerCase(java.util.Locale.ROOT).contains(keyword)) {
      return true;
    }
    for (SessionMessage message : messages) {
      if (message.content().toLowerCase(java.util.Locale.ROOT).contains(keyword)) {
        return true;
      }
    }
    return false;
  }
}
