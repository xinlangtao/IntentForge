package cn.intentforge.agent.core;

import java.util.List;

/**
 * Ordered route selected for one task execution.
 *
 * @param strategy routing strategy identifier
 * @param steps ordered route steps
 */
public record AgentRoute(
    String strategy,
    List<AgentRouteStep> steps
) {
  /**
   * Creates a validated route.
   */
  public AgentRoute {
    strategy = cn.intentforge.common.util.ValidationSupport.requireText(strategy, "strategy");
    steps = AgentModelSupport.immutableList(steps, "steps");
    if (steps.isEmpty()) {
      throw new IllegalArgumentException("steps must not be empty");
    }
  }
}
