package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.immutableStringMap;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * Represents one upstream webhook status snapshot.
 *
 * @param url currently configured webhook URL
 * @param metadata optional platform-specific status fields
 * @since 1.0.0
 */
public record ChannelWebhookStatus(
    URI url,
    Map<String, String> metadata
) {
  /**
   * Creates one validated immutable webhook status snapshot.
   */
  public ChannelWebhookStatus {
    url = Objects.requireNonNull(url, "url must not be null");
    metadata = immutableStringMap(metadata, "metadata");
  }
}
