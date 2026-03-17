package cn.intentforge.channel.telegram.inbound;

import java.util.Locale;

/**
 * Supported Telegram inbound ingestion modes.
 *
 * @since 1.0.0
 */
public enum TelegramInboundMode {
  /**
   * Uses Telegram Bot API {@code getUpdates}.
   */
  LONG_POLLING,
  /**
   * Uses Telegram webhook callbacks.
   */
  WEBHOOK;

  /**
   * Parses the configured inbound mode and defaults to {@link #LONG_POLLING} when absent.
   *
   * @param rawValue configured inbound mode text
   * @return parsed inbound mode
   */
  public static TelegramInboundMode from(String rawValue) {
    if (rawValue == null || rawValue.isBlank()) {
      return LONG_POLLING;
    }
    return TelegramInboundMode.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
  }
}
