package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.immutableStringMap;

import java.util.Map;

/**
 * Describes one webhook deletion request.
 *
 * @param dropPendingUpdates whether queued platform updates should be dropped during deletion
 * @param metadata optional platform-specific deletion metadata
 * @since 1.0.0
 */
public record ChannelWebhookDeletion(
    boolean dropPendingUpdates,
    Map<String, String> metadata
) {
  /**
   * Creates one validated immutable webhook deletion request.
   */
  public ChannelWebhookDeletion {
    metadata = immutableStringMap(metadata, "metadata");
  }
}
