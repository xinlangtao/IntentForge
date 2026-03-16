package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.immutableStringMap;
import static cn.intentforge.common.util.ValidationSupport.normalizeOptional;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Represents one HTTP-style response returned after webhook handling.
 *
 * @param statusCode HTTP status code
 * @param contentType response content type
 * @param body optional response body
 * @param headers optional response headers
 * @since 1.0.0
 */
public record ChannelWebhookResponse(
    int statusCode,
    String contentType,
    String body,
    Map<String, String> headers
) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Creates one validated immutable webhook response.
   */
  public ChannelWebhookResponse {
    if (statusCode < 100 || statusCode > 599) {
      throw new IllegalArgumentException("statusCode must be between 100 and 599");
    }
    contentType = requireText(contentType, "contentType");
    body = normalizeOptional(body);
    headers = immutableStringMap(headers, "headers");
  }
}
