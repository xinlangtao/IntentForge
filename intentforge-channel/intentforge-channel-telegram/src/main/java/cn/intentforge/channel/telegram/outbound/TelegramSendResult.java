package cn.intentforge.channel.telegram.outbound;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.time.Instant;
import java.util.Objects;

/**
 * Telegram outbound send result.
 *
 * @param messageId Telegram message identifier
 * @param acceptedAt acceptance timestamp reported by Telegram
 * @since 1.0.0
 */
public record TelegramSendResult(
    String messageId,
    Instant acceptedAt
) {
  /**
   * Creates one validated Telegram send result.
   */
  public TelegramSendResult {
    messageId = requireText(messageId, "messageId");
    acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt must not be null");
  }
}
