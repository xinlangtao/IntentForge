package cn.intentforge.channel.telegram.admin;

import cn.intentforge.channel.ChannelWebhookAdministration;
import cn.intentforge.channel.ChannelWebhookDeletion;
import cn.intentforge.channel.ChannelWebhookRegistration;
import cn.intentforge.channel.ChannelWebhookStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Account-bound Telegram webhook lifecycle administration.
 *
 * @since 1.0.0
 */
public final class TelegramWebhookAdministration implements ChannelWebhookAdministration {
  private final String baseUrl;
  private final String botToken;
  private final TelegramWebhookApiClient apiClient;

  /**
   * Creates one account-bound webhook administration facade.
   *
   * @param baseUrl Telegram Bot API base URL
   * @param botToken Telegram bot token
   * @param apiClient webhook administration client
   */
  public TelegramWebhookAdministration(String baseUrl, String botToken, TelegramWebhookApiClient apiClient) {
    this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
    this.botToken = Objects.requireNonNull(botToken, "botToken must not be null");
    this.apiClient = Objects.requireNonNull(apiClient, "apiClient must not be null");
  }

  @Override
  public void setWebhook(ChannelWebhookRegistration registration) {
    Objects.requireNonNull(registration, "registration must not be null");
    apiClient.setWebhook(new TelegramSetWebhookCommand(
        baseUrl,
        botToken,
        registration.url(),
        registration.secretToken(),
        registration.eventTypes(),
        registration.maxConnections(),
        registration.dropPendingUpdates()));
  }

  @Override
  public void deleteWebhook(ChannelWebhookDeletion deletion) {
    Objects.requireNonNull(deletion, "deletion must not be null");
    apiClient.deleteWebhook(new TelegramDeleteWebhookCommand(baseUrl, botToken, deletion.dropPendingUpdates()));
  }

  @Override
  public Optional<ChannelWebhookStatus> getWebhookInfo() {
    TelegramWebhookInfo info = apiClient.getWebhookInfo(new TelegramWebhookInfoCommand(baseUrl, botToken));
    Map<String, String> metadata = new LinkedHashMap<>();
    if (info.pendingUpdateCount() != null) {
      metadata.put("pendingUpdateCount", String.valueOf(info.pendingUpdateCount()));
    }
    if (info.maxConnections() != null) {
      metadata.put("maxConnections", String.valueOf(info.maxConnections()));
    }
    if (info.ipAddress() != null) {
      metadata.put("ipAddress", info.ipAddress());
    }
    if (info.lastErrorAt() != null) {
      metadata.put("lastErrorAt", String.valueOf(info.lastErrorAt().getEpochSecond()));
    }
    if (info.lastErrorMessage() != null) {
      metadata.put("lastErrorMessage", info.lastErrorMessage());
    }
    if (!info.allowedUpdates().isEmpty()) {
      metadata.put("eventTypes", String.join(",", info.allowedUpdates()));
    }
    return Optional.of(new ChannelWebhookStatus(info.url(), Map.copyOf(metadata)));
  }
}
