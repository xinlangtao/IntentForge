package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.immutableList;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Represents the aggregated inbound processing result for one webhook request.
 *
 * @param response webhook acknowledgement response
 * @param dispatches ordered inbound dispatch outcomes
 * @since 1.0.0
 */
public record ChannelInboundProcessingResult(
    ChannelWebhookResponse response,
    List<ChannelInboundDispatch> dispatches
) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Creates one validated immutable inbound processing result.
   */
  public ChannelInboundProcessingResult {
    response = Objects.requireNonNull(response, "response must not be null");
    dispatches = immutableList(dispatches, "dispatches");
  }
}
