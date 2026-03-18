package cn.intentforge.channel.wecom.outbound;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelDeliveryResult;
import cn.intentforge.channel.ChannelOutboundRequest;
import cn.intentforge.channel.ChannelSession;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Account-bound WeCom intelligent-robot outbound session.
 *
 * @since 1.0.0
 */
public final class WeComChannelSession implements ChannelSession {
  private final ChannelAccountProfile accountProfile;
  private final String baseUrl;
  private final String robotId;
  private final String robotSecret;
  private final WeComRobotApiClient apiClient;

  /**
   * Creates one outbound session.
   *
   * @param accountProfile WeCom account profile
   * @param baseUrl WeCom API base URL
   * @param robotId robot identifier
   * @param robotSecret robot secret
   * @param apiClient outbound API client
   */
  public WeComChannelSession(
      ChannelAccountProfile accountProfile,
      String baseUrl,
      String robotId,
      String robotSecret,
      WeComRobotApiClient apiClient
  ) {
    this.accountProfile = Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
    this.robotId = Objects.requireNonNull(robotId, "robotId must not be null");
    this.robotSecret = Objects.requireNonNull(robotSecret, "robotSecret must not be null");
    this.apiClient = Objects.requireNonNull(apiClient, "apiClient must not be null");
  }

  @Override
  public ChannelAccountProfile accountProfile() {
    return accountProfile;
  }

  @Override
  public ChannelDeliveryResult send(ChannelOutboundRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    String chatId = firstNonBlank(request.target().conversationId(), metadataText(request.metadata(), "chatId"));
    String userId = firstNonBlank(request.target().recipientId(), metadataText(request.metadata(), "userId"));
    String sessionId = metadataText(request.metadata(), "sessionId");
    WeComRobotSendMessageResult sendResult = apiClient.sendText(new WeComRobotSendMessageCommand(
        baseUrl,
        robotId,
        robotSecret,
        chatId,
        userId,
        sessionId,
        request.text()));
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("driver", cn.intentforge.channel.wecom.driver.WeComChannelDriver.DRIVER_ID);
    putIfPresent(metadata, "chatId", chatId);
    putIfPresent(metadata, "userId", userId);
    putIfPresent(metadata, "sessionId", sessionId);
    return new ChannelDeliveryResult(
        "wecom-robot:" + sendResult.messageId(),
        sendResult.messageId(),
        sendResult.acceptedAt(),
        Map.copyOf(metadata));
  }

  private static String metadataText(Map<String, Object> metadata, String key) {
    if (metadata == null || metadata.isEmpty() || metadata.get(key) == null) {
      return null;
    }
    return normalize(String.valueOf(metadata.get(key)));
  }

  private static String firstNonBlank(String first, String second) {
    String normalizedFirst = normalize(first);
    return normalizedFirst == null ? normalize(second) : normalizedFirst;
  }

  private static void putIfPresent(Map<String, Object> target, String key, String value) {
    if (value != null) {
      target.put(key, value);
    }
  }
}
