package cn.intentforge.channel.telegram.outbound;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelDeliveryResult;
import cn.intentforge.channel.ChannelOutboundRequest;
import cn.intentforge.channel.ChannelSession;
import cn.intentforge.channel.telegram.driver.TelegramChannelDriver;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Account-bound Telegram outbound session.
 *
 * @since 1.0.0
 */
public final class TelegramChannelSession implements ChannelSession {
  private final ChannelAccountProfile accountProfile;
  private final String baseUrl;
  private final String botToken;
  private final TelegramBotApiClient apiClient;

  public TelegramChannelSession(
      ChannelAccountProfile accountProfile,
      String baseUrl,
      String botToken,
      TelegramBotApiClient apiClient
  ) {
    this.accountProfile = Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
    this.botToken = Objects.requireNonNull(botToken, "botToken must not be null");
    this.apiClient = Objects.requireNonNull(apiClient, "apiClient must not be null");
  }

  @Override
  public ChannelAccountProfile accountProfile() {
    return accountProfile;
  }

  @Override
  public ChannelDeliveryResult send(ChannelOutboundRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    TelegramSendResult sendResult = apiClient.sendMessage(new TelegramSendCommand(
        baseUrl,
        botToken,
        request.target().conversationId(),
        firstNonBlank(request.target().threadId(), metadataText(request.metadata(), "messageThreadId")),
        request.text(),
        metadataText(request.metadata(), "parseMode"),
        metadataBoolean(request.metadata(), "disableNotification"),
        metadataBoolean(request.metadata(), "disableWebPagePreview")));
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("driver", TelegramChannelDriver.DRIVER_ID);
    metadata.put("chatId", request.target().conversationId());
    if (request.target().threadId() != null) {
      metadata.put("messageThreadId", request.target().threadId());
    }
    return new ChannelDeliveryResult(
        "telegram:" + sendResult.messageId(),
        sendResult.messageId(),
        sendResult.acceptedAt(),
        Map.copyOf(metadata));
  }

  private static String metadataText(Map<String, Object> metadata, String key) {
    if (metadata == null || metadata.isEmpty()) {
      return null;
    }
    Object value = metadata.get(key);
    return value == null ? null : normalize(String.valueOf(value));
  }

  private static boolean metadataBoolean(Map<String, Object> metadata, String key) {
    if (metadata == null || metadata.isEmpty()) {
      return false;
    }
    Object value = metadata.get(key);
    if (value instanceof Boolean booleanValue) {
      return booleanValue;
    }
    return value != null && Boolean.parseBoolean(String.valueOf(value));
  }

  private static String firstNonBlank(String preferred, String fallback) {
    String preferredValue = normalize(preferred);
    return preferredValue == null ? normalize(fallback) : preferredValue;
  }
}
