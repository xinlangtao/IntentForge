package cn.intentforge.channel.telegram;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * Describes one Telegram Bot API {@code setWebhook} request.
 *
 * @param baseUrl Telegram Bot API base URL
 * @param botToken Telegram bot token
 * @param webhookUrl externally reachable webhook URL
 * @param secretToken optional webhook secret token
 * @param allowedUpdates optional subscribed update kinds
 * @param maxConnections optional concurrent connection limit
 * @param dropPendingUpdates whether queued updates should be dropped
 * @since 1.0.0
 */
record TelegramSetWebhookCommand(
    String baseUrl,
    String botToken,
    URI webhookUrl,
    String secretToken,
    List<String> allowedUpdates,
    Integer maxConnections,
    boolean dropPendingUpdates
) {
  TelegramSetWebhookCommand {
    baseUrl = requireText(baseUrl, "baseUrl");
    botToken = requireText(botToken, "botToken");
    webhookUrl = Objects.requireNonNull(webhookUrl, "webhookUrl must not be null");
    allowedUpdates = allowedUpdates == null ? List.of() : List.copyOf(allowedUpdates);
    if (maxConnections != null && maxConnections <= 0) {
      throw new IllegalArgumentException("maxConnections must be greater than 0");
    }
  }
}
