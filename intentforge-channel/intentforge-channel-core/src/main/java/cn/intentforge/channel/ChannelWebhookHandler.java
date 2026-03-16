package cn.intentforge.channel;

import java.util.Objects;

/**
 * Handles one inbound channel webhook request and normalizes the result.
 *
 * @since 1.0.0
 */
public interface ChannelWebhookHandler {
  /**
   * Handles one webhook request.
   *
   * @param request webhook request
   * @return normalized webhook result
   */
  ChannelWebhookResult handle(ChannelWebhookRequest request);

  /**
   * Returns one handler that always responds with the provided result.
   *
   * @param result fixed result
   * @return fixed webhook handler
   */
  static ChannelWebhookHandler fixed(ChannelWebhookResult result) {
    ChannelWebhookResult nonNullResult = Objects.requireNonNull(result, "result must not be null");
    return request -> nonNullResult;
  }
}
