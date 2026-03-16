package cn.intentforge.channel.connectors.wecom;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.time.Instant;
import java.util.Objects;

record WeComSendResult(
    String messageId,
    Instant acceptedAt
) {
  WeComSendResult {
    messageId = requireText(messageId, "messageId");
    acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt must not be null");
  }
}
