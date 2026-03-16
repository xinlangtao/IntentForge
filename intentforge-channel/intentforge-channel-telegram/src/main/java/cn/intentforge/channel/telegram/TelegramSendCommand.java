package cn.intentforge.channel.telegram;

import static cn.intentforge.common.util.ValidationSupport.normalizeOptional;
import static cn.intentforge.common.util.ValidationSupport.requireText;

record TelegramSendCommand(
    String baseUrl,
    String botToken,
    String chatId,
    String messageThreadId,
    String text,
    String parseMode,
    boolean disableNotification,
    boolean disableWebPagePreview
) {
  TelegramSendCommand {
    baseUrl = requireText(baseUrl, "baseUrl");
    botToken = requireText(botToken, "botToken");
    chatId = requireText(chatId, "chatId");
    messageThreadId = normalizeOptional(messageThreadId);
    text = requireText(text, "text");
    parseMode = normalizeOptional(parseMode);
  }
}
