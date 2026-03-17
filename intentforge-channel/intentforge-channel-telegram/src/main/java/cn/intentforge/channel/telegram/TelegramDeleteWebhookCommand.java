package cn.intentforge.channel.telegram;

import static cn.intentforge.common.util.ValidationSupport.requireText;

/**
 * Describes one Telegram Bot API {@code deleteWebhook} request.
 *
 * @param baseUrl Telegram Bot API base URL
 * @param botToken Telegram bot token
 * @param dropPendingUpdates whether queued updates should be dropped
 * @since 1.0.0
 */
record TelegramDeleteWebhookCommand(
    String baseUrl,
    String botToken,
    boolean dropPendingUpdates
) {
  TelegramDeleteWebhookCommand {
    baseUrl = requireText(baseUrl, "baseUrl");
    botToken = requireText(botToken, "botToken");
  }
}
