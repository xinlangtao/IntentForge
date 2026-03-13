package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.immutableStringMap;
import static cn.intentforge.common.util.ValidationSupport.normalizeOptional;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Defines one configured channel account instance.
 *
 * @param id stable account identifier
 * @param type channel transport type
 * @param displayName human-readable account name
 * @param properties driver-specific account properties
 * @since 1.0.0
 */
public record ChannelAccountProfile(
    String id,
    ChannelType type,
    String displayName,
    Map<String, String> properties
) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Creates one validated immutable account profile.
   */
  public ChannelAccountProfile {
    id = requireText(id, "id");
    type = Objects.requireNonNull(type, "type must not be null");
    displayName = normalizeOptional(displayName);
    properties = immutableStringMap(properties, "properties");
  }
}
