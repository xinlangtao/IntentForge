package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.immutableObjectMap;
import static cn.intentforge.common.util.ValidationSupport.normalizeOptional;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the normalized result of one outbound channel delivery.
 *
 * @param deliveryId internal delivery identifier
 * @param externalMessageId optional transport message identifier
 * @param acceptedAt accepted timestamp
 * @param metadata transport-specific metadata
 * @since 1.0.0
 */
public record ChannelDeliveryResult(
    String deliveryId,
    String externalMessageId,
    Instant acceptedAt,
    Map<String, Object> metadata
) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Creates one validated immutable delivery result.
   */
  public ChannelDeliveryResult {
    deliveryId = requireText(deliveryId, "deliveryId");
    externalMessageId = normalizeOptional(externalMessageId);
    acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt must not be null");
    metadata = immutableObjectMap(metadata, "metadata");
  }
}
