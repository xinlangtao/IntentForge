package cn.intentforge.channel.wecom.outbound;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents the outbound result returned by the WeCom intelligent-robot send API.
 *
 * @param messageId platform message identifier
 * @param acceptedAt accepted timestamp
 * @since 1.0.0
 */
public record WeComRobotSendMessageResult(
    String messageId,
    Instant acceptedAt
) {
  /**
   * Creates one validated immutable send result.
   */
  public WeComRobotSendMessageResult {
    messageId = requireText(messageId, "messageId");
    acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt must not be null");
  }
}
