package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.immutableObjectMap;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Represents one normalized outbound channel request.
 *
 * @param target normalized target information
 * @param text outbound text body
 * @param metadata transport-specific metadata
 * @since 1.0.0
 */
public record ChannelOutboundRequest(
    ChannelTarget target,
    String text,
    Map<String, Object> metadata
) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Creates one validated immutable outbound request.
   */
  public ChannelOutboundRequest {
    target = Objects.requireNonNull(target, "target must not be null");
    text = requireText(text, "text");
    metadata = immutableObjectMap(metadata, "metadata");
  }
}
