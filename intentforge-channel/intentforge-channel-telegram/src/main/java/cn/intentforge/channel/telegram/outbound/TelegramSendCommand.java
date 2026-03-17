package cn.intentforge.channel.telegram.outbound;

import static cn.intentforge.common.util.ValidationSupport.normalizeOptional;
import static cn.intentforge.common.util.ValidationSupport.requireText;

/**
 * Telegram outbound send command.
 *
 * @param baseUrl Telegram Bot API base URL
 * @param botToken Telegram bot token
 * @param chatId target chat identifier
 * @param messageThreadId optional thread identifier
 * @param text message text
 * @param parseMode optional parse mode
 * @param disableNotification whether notifications are disabled
 * @param disableWebPagePreview whether link previews are disabled
 * @since 1.0.0
 */
public record TelegramSendCommand(
    String baseUrl,
    String botToken,
    String chatId,
    String messageThreadId,
    String text,
    String parseMode,
    boolean disableNotification,
    boolean disableWebPagePreview
) {
  /**
   * Creates one validated Telegram send command.
   */
  public TelegramSendCommand {
    baseUrl = requireText(baseUrl, "baseUrl");
    botToken = requireText(botToken, "botToken");
    chatId = requireText(chatId, "chatId");
    messageThreadId = normalizeOptional(messageThreadId);
    text = requireText(text, "text");
    parseMode = normalizeOptional(parseMode);
  }
}
