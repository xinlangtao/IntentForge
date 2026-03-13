package cn.intentforge.agent.core;

import java.util.Objects;

/**
 * One ordered route step selected by governance.
 *
 * @param order one-based execution order
 * @param agentId selected agent identifier
 * @param role selected agent role
 * @param reason short routing rationale
 */
public record AgentRouteStep(
    int order,
    String agentId,
    AgentRole role,
    String reason
) {
  /**
   * Creates a validated route step.
   */
  public AgentRouteStep {
    if (order < 1) {
      throw new IllegalArgumentException("order must be greater than zero");
    }
    agentId = cn.intentforge.common.util.ValidationSupport.requireText(agentId, "agentId");
    role = Objects.requireNonNull(role, "role must not be null");
    reason = cn.intentforge.common.util.ValidationSupport.requireText(reason, "reason");
  }
}
