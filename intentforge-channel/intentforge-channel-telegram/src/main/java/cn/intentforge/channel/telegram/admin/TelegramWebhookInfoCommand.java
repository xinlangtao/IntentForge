package cn.intentforge.channel.telegram.admin;

import static cn.intentforge.common.util.ValidationSupport.requireText;

/**
 * Describes one Telegram Bot API {@code getWebhookInfo} request.
 *
 * @param baseUrl Telegram Bot API base URL
 * @param botToken Telegram bot token
 * @since 1.0.0
 */
public record TelegramWebhookInfoCommand(
    String baseUrl,
    String botToken
) {
  /**
   * Creates one validated Telegram webhook information lookup command.
   */
  public TelegramWebhookInfoCommand {
    baseUrl = requireText(baseUrl, "baseUrl");
    botToken = requireText(botToken, "botToken");
  }
}
