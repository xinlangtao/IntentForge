package cn.intentforge.channel.telegram.outbound;

/**
 * Telegram Bot API client for outbound message delivery.
 *
 * @since 1.0.0
 */
public interface TelegramBotApiClient {
  /**
   * Sends one Telegram text message.
   *
   * @param command outbound send command
   * @return Telegram send result
   */
  TelegramSendResult sendMessage(TelegramSendCommand command);
}
