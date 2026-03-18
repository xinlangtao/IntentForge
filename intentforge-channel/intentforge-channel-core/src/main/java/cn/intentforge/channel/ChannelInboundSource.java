package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.immutableObjectMap;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Describes where one batch of normalized inbound channel messages originated.
 *
 * @param type ingress transport type
 * @param attributes source-specific attributes such as webhook method or polling batch metadata
 * @since 1.0.0
 */
public record ChannelInboundSource(
    ChannelInboundSourceType type,
    Map<String, Object> attributes
) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Creates one validated immutable inbound source descriptor.
   */
  public ChannelInboundSource {
    type = Objects.requireNonNull(type, "type must not be null");
    attributes = immutableObjectMap(attributes, "attributes");
  }
}
