package cn.intentforge.channel.connectors.wecom;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.time.Instant;
import java.util.Objects;

record WeComAccessTokenResult(
    String accessToken,
    Instant expiresAt
) {
  WeComAccessTokenResult {
    accessToken = requireText(accessToken, "accessToken");
    expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
  }
}
