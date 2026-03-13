package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.immutableStringMap;
import static cn.intentforge.common.util.ValidationSupport.normalizeOptional;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Represents the routing decision that maps an inbound message into the IntentForge runtime.
 *
 * @param spaceId resolved space identifier
 * @param sessionId resolved session identifier
 * @param agentId optional resolved agent identifier
 * @param attributes routing attributes for observability
 * @since 1.0.0
 */
public record ChannelRouteDecision(
    String spaceId,
    String sessionId,
    String agentId,
    Map<String, String> attributes
) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Creates one validated immutable route decision.
   */
  public ChannelRouteDecision {
    spaceId = requireText(spaceId, "spaceId");
    sessionId = requireText(sessionId, "sessionId");
    agentId = normalizeOptional(agentId);
    attributes = immutableStringMap(attributes, "attributes");
  }
}
