package cn.intentforge.channel;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the processing outcome for one normalized inbound channel message.
 *
 * @param message normalized inbound message
 * @param accessDecision access-control decision
 * @param routeDecision optional route decision for allowed messages
 * @since 1.0.0
 */
public record ChannelInboundDispatch(
    ChannelInboundMessage message,
    ChannelAccessDecision accessDecision,
    Optional<ChannelRouteDecision> routeDecision
) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Creates one validated immutable inbound dispatch outcome.
   */
  public ChannelInboundDispatch {
    message = Objects.requireNonNull(message, "message must not be null");
    accessDecision = Objects.requireNonNull(accessDecision, "accessDecision must not be null");
    routeDecision = Objects.requireNonNull(routeDecision, "routeDecision must not be null");
  }
}
