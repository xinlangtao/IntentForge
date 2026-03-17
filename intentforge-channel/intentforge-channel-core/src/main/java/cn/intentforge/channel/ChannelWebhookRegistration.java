package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Describes one desired webhook registration state.
 *
 * @param url externally reachable webhook URL
 * @param secretToken optional webhook secret token
 * @param eventTypes optional subscribed event types
 * @param maxConnections optional concurrent connection limit
 * @param dropPendingUpdates whether queued platform updates should be dropped during registration
 * @param metadata optional platform-specific metadata
 * @since 1.0.0
 */
public record ChannelWebhookRegistration(
    URI url,
    String secretToken,
    List<String> eventTypes,
    Integer maxConnections,
    boolean dropPendingUpdates,
    Map<String, String> metadata
) {
  /**
   * Creates one validated immutable webhook registration description.
   */
  public ChannelWebhookRegistration {
    url = Objects.requireNonNull(url, "url must not be null");
    requireText(url.toString(), "url");
    secretToken = normalizeOptional(secretToken);
    eventTypes = immutableTextList(eventTypes, "eventTypes");
    if (maxConnections != null && maxConnections <= 0) {
      throw new IllegalArgumentException("maxConnections must be greater than 0");
    }
    metadata = immutableStringMap(metadata, "metadata");
  }

  private static List<String> immutableTextList(List<String> values, String fieldName) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    return List.copyOf(values.stream().map(value -> requireText(value, fieldName + " item")).toList());
  }

  private static String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static Map<String, String> immutableStringMap(Map<String, String> values, String fieldName) {
    if (values == null || values.isEmpty()) {
      return Map.of();
    }
    return Map.copyOf(values.entrySet()
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            entry -> requireText(entry.getKey(), fieldName + " key"),
            entry -> requireText(entry.getValue(), fieldName + " value"))));
  }
}
