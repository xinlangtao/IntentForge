package cn.intentforge.session.model;

/**
 * Query filters used when listing sessions.
 *
 * <p>This type is immutable and thread-safe.
 *
 * @param spaceId optional space filter; when provided it must not be blank
 * @param status optional lifecycle status filter
 * @param keyword optional case-insensitive keyword matched against title and message content
 */
public record SessionQuery(
    String spaceId,
    SessionStatus status,
    String keyword
) {
  /**
   * Creates a validated immutable query filter.
   */
  public SessionQuery {
    if (spaceId != null && spaceId.trim().isEmpty()) {
      throw new IllegalArgumentException("spaceId must not be blank");
    }
    spaceId = SessionModelSupport.normalize(spaceId);
    keyword = SessionModelSupport.normalizeKeyword(keyword);
  }
}
