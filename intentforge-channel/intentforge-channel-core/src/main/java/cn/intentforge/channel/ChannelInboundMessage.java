package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.immutableObjectMap;
import static cn.intentforge.common.util.ValidationSupport.normalizeOptional;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Represents one normalized inbound channel message.
 *
 * @param messageId stable inbound message identifier
 * @param accountId channel account identifier
 * @param type channel transport type
 * @param target normalized target information
 * @param sender normalized sender information
 * @param text normalized text body
 * @param metadata transport-specific metadata
 * @since 1.0.0
 */
public record ChannelInboundMessage(
    String messageId,
    String accountId,
    ChannelType type,
    ChannelTarget target,
    ChannelParticipant sender,
    String text,
    Map<String, Object> metadata
) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Creates one validated immutable inbound message.
   */
  public ChannelInboundMessage {
    messageId = requireText(messageId, "messageId");
    accountId = requireText(accountId, "accountId");
    type = Objects.requireNonNull(type, "type must not be null");
    target = Objects.requireNonNull(target, "target must not be null");
    sender = Objects.requireNonNull(sender, "sender must not be null");
    text = normalizeOptional(text);
    metadata = immutableObjectMap(metadata, "metadata");
  }
}
