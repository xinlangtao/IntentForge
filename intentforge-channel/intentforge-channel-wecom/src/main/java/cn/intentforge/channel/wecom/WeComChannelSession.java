package cn.intentforge.channel.wecom;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelDeliveryResult;
import cn.intentforge.channel.ChannelOutboundRequest;
import cn.intentforge.channel.ChannelSession;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class WeComChannelSession implements ChannelSession {
  private final ChannelAccountProfile accountProfile;
  private final String baseUrl;
  private final String corpId;
  private final String agentId;
  private final String corpSecret;
  private final WeComApplicationApiClient apiClient;

  private volatile WeComAccessTokenResult cachedAccessToken;

  WeComChannelSession(
      ChannelAccountProfile accountProfile,
      String baseUrl,
      String corpId,
      String agentId,
      String corpSecret,
      WeComApplicationApiClient apiClient
  ) {
    this.accountProfile = Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
    this.corpId = Objects.requireNonNull(corpId, "corpId must not be null");
    this.agentId = Objects.requireNonNull(agentId, "agentId must not be null");
    this.corpSecret = Objects.requireNonNull(corpSecret, "corpSecret must not be null");
    this.apiClient = Objects.requireNonNull(apiClient, "apiClient must not be null");
  }

  @Override
  public ChannelAccountProfile accountProfile() {
    return accountProfile;
  }

  @Override
  public ChannelDeliveryResult send(ChannelOutboundRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    String toUser = firstNonBlank(request.target().recipientId(), request.target().conversationId(), metadataText(request.metadata(), "toUser"));
    String toParty = metadataText(request.metadata(), "toParty");
    String toTag = metadataText(request.metadata(), "toTag");
    int safe = metadataInt(request.metadata(), "safe", 0);
    WeComAccessTokenResult accessToken = accessToken();
    WeComSendResult sendResult = apiClient.sendText(new WeComSendCommand(
        baseUrl,
        accessToken.accessToken(),
        agentId,
        toUser,
        toParty,
        toTag,
        request.text(),
        safe));
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("driver", WeComChannelDriver.DRIVER_ID);
    metadata.put("toUser", toUser);
    if (toParty != null) {
      metadata.put("toParty", toParty);
    }
    if (toTag != null) {
      metadata.put("toTag", toTag);
    }
    return new ChannelDeliveryResult(
        "wecom:" + sendResult.messageId(),
        sendResult.messageId(),
        sendResult.acceptedAt(),
        Map.copyOf(metadata));
  }

  private synchronized WeComAccessTokenResult accessToken() {
    WeComAccessTokenResult current = cachedAccessToken;
    Instant now = Instant.now();
    if (current == null || current.expiresAt().minusSeconds(30).isBefore(now)) {
      cachedAccessToken = apiClient.fetchAccessToken(new WeComAccessTokenCommand(baseUrl, corpId, corpSecret));
    }
    return cachedAccessToken;
  }

  private static String metadataText(Map<String, Object> metadata, String key) {
    if (metadata == null || metadata.isEmpty()) {
      return null;
    }
    Object value = metadata.get(key);
    return value == null ? null : normalize(String.valueOf(value));
  }

  private static int metadataInt(Map<String, Object> metadata, String key, int defaultValue) {
    if (metadata == null || metadata.isEmpty() || metadata.get(key) == null) {
      return defaultValue;
    }
    Object value = metadata.get(key);
    if (value instanceof Number number) {
      return number.intValue();
    }
    return Integer.parseInt(String.valueOf(value));
  }

  private static String firstNonBlank(String first, String second, String third) {
    String firstValue = normalize(first);
    if (firstValue != null) {
      return firstValue;
    }
    String secondValue = normalize(second);
    if (secondValue != null) {
      return secondValue;
    }
    return normalize(third);
  }
}
