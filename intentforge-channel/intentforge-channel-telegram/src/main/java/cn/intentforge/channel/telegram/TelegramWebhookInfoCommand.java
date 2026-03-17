package cn.intentforge.channel.telegram;

import static cn.intentforge.common.util.ValidationSupport.requireText;

/**
 * Describes one Telegram Bot API {@code getWebhookInfo} request.
 *
 * @param baseUrl Telegram Bot API base URL
 * @param botToken Telegram bot token
 * @since 1.0.0
 */
record TelegramWebhookInfoCommand(
    String baseUrl,
    String botToken
) {
  TelegramWebhookInfoCommand {
    baseUrl = requireText(baseUrl, "baseUrl");
    botToken = requireText(botToken, "botToken");
  }
}
