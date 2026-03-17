package cn.intentforge.channel.telegram.admin;

/**
 * Telegram Bot API client for webhook lifecycle administration.
 *
 * @since 1.0.0
 */
public interface TelegramWebhookApiClient {
  /**
   * Calls Telegram Bot API {@code setWebhook}.
   *
   * @param command request command
   */
  void setWebhook(TelegramSetWebhookCommand command);

  /**
   * Calls Telegram Bot API {@code deleteWebhook}.
   *
   * @param command request command
   */
  void deleteWebhook(TelegramDeleteWebhookCommand command);

  /**
   * Calls Telegram Bot API {@code getWebhookInfo}.
   *
   * @param command request command
   * @return current webhook information
   */
  TelegramWebhookInfo getWebhookInfo(TelegramWebhookInfoCommand command);
}
