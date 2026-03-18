package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.immutableList;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Represents the aggregated processing result for one batch of already normalized inbound messages.
 *
 * @param source batch ingress source descriptor
 * @param dispatches ordered inbound dispatch outcomes
 * @since 1.0.0
 */
public record ChannelInboundMessageProcessingResult(
    ChannelInboundSource source,
    List<ChannelInboundDispatch> dispatches
) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Creates one validated immutable message processing result.
   */
  public ChannelInboundMessageProcessingResult {
    source = Objects.requireNonNull(source, "source must not be null");
    dispatches = immutableList(dispatches, "dispatches");
  }
}
