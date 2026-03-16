package cn.intentforge.channel.telegram;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.time.Instant;
import java.util.Objects;

record TelegramSendResult(
    String messageId,
    Instant acceptedAt
) {
  TelegramSendResult {
    messageId = requireText(messageId, "messageId");
    acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt must not be null");
  }
}
