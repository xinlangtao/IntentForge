package cn.intentforge.api.agent;


/**
 * HTTP response that describes one selected route step in the run history.
 *
 * @param order one-based execution order
 * @param agentId selected agent identifier
 * @param role selected agent role name
 * @param reason short selection rationale
 */
public record AgentRouteStepResponse(
    int order,
    String agentId,
    String role,
    String reason
) {
  /**
   * Creates one validated route-step response.
   */
  public AgentRouteStepResponse {
    if (order < 1) {
      throw new IllegalArgumentException("order must be greater than zero");
    }
    agentId = cn.intentforge.common.util.ValidationSupport.requireText(agentId, "agentId");
    role = cn.intentforge.common.util.ValidationSupport.requireText(role, "role");
    reason = cn.intentforge.common.util.ValidationSupport.requireText(reason, "reason");
  }
}
