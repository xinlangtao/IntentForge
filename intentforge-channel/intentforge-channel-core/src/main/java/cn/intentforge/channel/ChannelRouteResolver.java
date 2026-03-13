package cn.intentforge.channel;

import java.util.Optional;

/**
 * Resolves one inbound channel message into a space, session, and optional agent target.
 *
 * @since 1.0.0
 */
public interface ChannelRouteResolver {
  /**
   * Resolves one inbound message into a routing decision.
   *
   * @param message inbound message
   * @return route decision when one can be resolved
   */
  Optional<ChannelRouteDecision> resolve(ChannelInboundMessage message);
}
