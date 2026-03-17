package cn.intentforge.channel.telegram.config;

/**
 * Telegram-specific account-property names used by the connector runtime.
 *
 * @since 1.0.0
 */
public final class TelegramChannelPropertyNames {
  /**
   * Telegram bot token property.
   */
  public static final String BOT_TOKEN = "botToken";
  /**
   * Telegram Bot API base URL property.
   */
  public static final String BASE_URL = "baseUrl";
  /**
   * Telegram inbound mode property.
   */
  public static final String INBOUND_MODE = "inboundMode";
  /**
   * Telegram long-polling allowed-updates property.
   */
  public static final String POLLING_ALLOWED_UPDATES = "pollingAllowedUpdates";
  /**
   * Telegram long-polling startup toggle that controls whether an existing webhook is deleted before polling starts.
   */
  public static final String POLLING_DELETE_WEBHOOK_ON_START = "pollingDeleteWebhookOnStart";

  private TelegramChannelPropertyNames() {
  }
}
