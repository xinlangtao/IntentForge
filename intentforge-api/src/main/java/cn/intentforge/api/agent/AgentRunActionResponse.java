package cn.intentforge.api.agent;

import cn.intentforge.api.util.ApiModelSupport;

/**
 * HTTP response that describes one selectable next action for a paused run.
 *
 * @param agentId target agent identifier when the action continues execution with one agent, otherwise {@code null}
 * @param role target role name when the action continues execution with one agent, otherwise {@code null}
 * @param preferred whether governance marks this action as the preferred default
 * @param complete whether this action completes the run
 * @param reason short rationale shown to the caller
 */
public record AgentRunActionResponse(
    String agentId,
    String role,
    boolean preferred,
    boolean complete,
    String reason
) {
  /**
   * Creates one validated action response.
   */
  public AgentRunActionResponse {
    reason = ApiModelSupport.requireText(reason, "reason");
    if (complete) {
      agentId = ApiModelSupport.normalize(agentId);
      role = ApiModelSupport.normalize(role);
    } else {
      agentId = ApiModelSupport.requireText(agentId, "agentId");
      role = ApiModelSupport.requireText(role, "role");
    }
  }
}
