package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.normalize;
import static cn.intentforge.common.util.ValidationSupport.normalizeOptional;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents one normalized inbound channel webhook request.
 *
 * @param method HTTP method
 * @param headers normalized request headers
 * @param queryParameters normalized query parameters
 * @param body optional request body
 * @since 1.0.0
 */
public record ChannelWebhookRequest(
    String method,
    Map<String, List<String>> headers,
    Map<String, List<String>> queryParameters,
    String body
) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Creates one validated immutable webhook request.
   */
  public ChannelWebhookRequest {
    method = requireText(method, "method").toUpperCase(java.util.Locale.ROOT);
    headers = immutableMultiMap(headers, "headers");
    queryParameters = immutableMultiMap(queryParameters, "queryParameters");
    body = normalizeOptional(body);
  }

  /**
   * Returns the first header value for the provided name.
   *
   * @param name header name
   * @return normalized first value when present
   */
  public String firstHeader(String name) {
    return firstValue(headers, name);
  }

  /**
   * Returns the first query parameter value for the provided name.
   *
   * @param name query parameter name
   * @return normalized first value when present
   */
  public String firstQueryParameter(String name) {
    return firstValue(queryParameters, name);
  }

  private static Map<String, List<String>> immutableMultiMap(Map<String, List<String>> source, String fieldName) {
    if (source == null || source.isEmpty()) {
      return Map.of();
    }
    Map<String, List<String>> normalized = new LinkedHashMap<>();
    for (Map.Entry<String, List<String>> entry : source.entrySet()) {
      Objects.requireNonNull(entry, fieldName + " entry must not be null");
      String key = requireText(entry.getKey(), fieldName + " key");
      List<String> values = entry.getValue();
      if (values == null || values.isEmpty()) {
        normalized.put(key, List.of());
        continue;
      }
      List<String> normalizedValues = new ArrayList<>();
      for (String value : values) {
        String normalizedValue = normalize(value);
        if (normalizedValue != null) {
          normalizedValues.add(normalizedValue);
        }
      }
      normalized.put(key, List.copyOf(normalizedValues));
    }
    return Map.copyOf(normalized);
  }

  private static String firstValue(Map<String, List<String>> valuesByKey, String key) {
    String normalizedKey = normalize(key);
    if (normalizedKey == null || valuesByKey == null || valuesByKey.isEmpty()) {
      return null;
    }
    for (Map.Entry<String, List<String>> entry : valuesByKey.entrySet()) {
      if (!entry.getKey().equalsIgnoreCase(normalizedKey) || entry.getValue().isEmpty()) {
        continue;
      }
      return entry.getValue().getFirst();
    }
    return null;
  }
}
