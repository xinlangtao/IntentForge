package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.immutableList;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Represents the normalized result of one inbound webhook handling pass.
 *
 * @param messages normalized inbound messages extracted from the webhook payload
 * @param response webhook acknowledgement response
 * @since 1.0.0
 */
public record ChannelWebhookResult(
    List<ChannelInboundMessage> messages,
    ChannelWebhookResponse response
) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Creates one validated immutable webhook result.
   */
  public ChannelWebhookResult {
    messages = immutableList(messages, "messages");
    response = Objects.requireNonNull(response, "response must not be null");
  }
}
