package cn.intentforge.channel.telegram.inbound.polling;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.util.List;

record TelegramGetUpdatesCommand(
    String baseUrl,
    String botToken,
    Long offset,
    int limit,
    int timeoutSeconds,
    List<String> allowedUpdates
) {
  TelegramGetUpdatesCommand {
    baseUrl = requireText(baseUrl, "baseUrl");
    botToken = requireText(botToken, "botToken");
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be positive");
    }
    if (timeoutSeconds < 0) {
      throw new IllegalArgumentException("timeoutSeconds must be non-negative");
    }
    allowedUpdates = allowedUpdates == null ? List.of() : List.copyOf(allowedUpdates);
  }
}
