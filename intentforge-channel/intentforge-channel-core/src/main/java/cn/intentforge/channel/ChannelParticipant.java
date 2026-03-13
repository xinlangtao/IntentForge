package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.immutableStringMap;
import static cn.intentforge.common.util.ValidationSupport.normalizeOptional;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Represents one normalized channel participant.
 *
 * @param id participant identifier
 * @param displayName optional participant display name
 * @param bot whether the participant is a bot account
 * @param attributes transport-specific participant attributes
 * @since 1.0.0
 */
public record ChannelParticipant(
    String id,
    String displayName,
    boolean bot,
    Map<String, String> attributes
) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Creates one validated immutable participant.
   */
  public ChannelParticipant {
    id = requireText(id, "id");
    displayName = normalizeOptional(displayName);
    attributes = immutableStringMap(attributes, "attributes");
  }
}
