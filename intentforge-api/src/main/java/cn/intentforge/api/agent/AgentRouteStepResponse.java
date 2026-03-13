package cn.intentforge.api.agent;

import cn.intentforge.api.util.ApiModelSupport;

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
    agentId = ApiModelSupport.requireText(agentId, "agentId");
    role = ApiModelSupport.requireText(role, "role");
    reason = ApiModelSupport.requireText(reason, "reason");
  }
}
