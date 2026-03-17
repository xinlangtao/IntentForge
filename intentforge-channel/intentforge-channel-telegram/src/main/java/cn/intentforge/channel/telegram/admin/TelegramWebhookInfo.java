package cn.intentforge.channel.telegram.admin;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Represents one Telegram Bot API {@code WebhookInfo} snapshot.
 *
 * @param url configured webhook URL
 * @param pendingUpdateCount pending update count
 * @param maxConnections configured maximum connections
 * @param ipAddress Telegram IP address currently used to deliver updates
 * @param lastErrorAt last delivery error timestamp
 * @param lastErrorMessage last delivery error message
 * @param allowedUpdates configured allowed updates
 * @since 1.0.0
 */
public record TelegramWebhookInfo(
    URI url,
    Integer pendingUpdateCount,
    Integer maxConnections,
    String ipAddress,
    Instant lastErrorAt,
    String lastErrorMessage,
    List<String> allowedUpdates
) {
  /**
   * Creates one validated Telegram webhook information snapshot.
   */
  public TelegramWebhookInfo {
    url = Objects.requireNonNull(url, "url must not be null");
    allowedUpdates = allowedUpdates == null ? List.of() : List.copyOf(allowedUpdates);
  }
}
