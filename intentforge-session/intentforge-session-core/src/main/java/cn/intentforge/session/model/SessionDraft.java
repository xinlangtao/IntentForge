package cn.intentforge.session.model;

import java.util.Map;

/**
 * Immutable command used to create a new session.
 *
 * <p>This type is immutable and thread-safe.
 *
 * @param id session identifier
 * @param title optional human readable title
 * @param spaceId optional owning space identifier
 * @param metadata session metadata copied into the created snapshot
 */
public record SessionDraft(
    String id,
    String title,
    String spaceId,
    Map<String, String> metadata
) {
  /**
   * Creates a validated immutable draft.
   */
  public SessionDraft {
    id = cn.intentforge.common.util.ValidationSupport.requireText(id, "id");
    title = cn.intentforge.common.util.ValidationSupport.normalize(title);
    spaceId = cn.intentforge.common.util.ValidationSupport.normalize(spaceId);
    metadata = SessionModelSupport.immutableMetadata(metadata);
  }
}
