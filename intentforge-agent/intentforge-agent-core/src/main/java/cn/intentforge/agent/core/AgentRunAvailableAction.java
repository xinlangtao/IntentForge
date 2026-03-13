package cn.intentforge.agent.core;

import java.util.Objects;

/**
 * One user-selectable action that can be chosen while a run is awaiting input.
 *
 * @param agentId target agent identifier when the action continues execution with one agent, otherwise {@code null}
 * @param role target agent role when the action continues execution with one agent, otherwise {@code null}
 * @param preferred whether governance marks this action as the preferred default
 * @param complete whether this action finishes the run instead of executing another agent
 * @param reason short rationale shown to the caller
 */
public record AgentRunAvailableAction(
    String agentId,
    AgentRole role,
    boolean preferred,
    boolean complete,
    String reason
) {
  /**
   * Creates one validated available action.
   */
  public AgentRunAvailableAction {
    reason = AgentModelSupport.requireText(reason, "reason");
    if (complete) {
      if (agentId != null || role != null) {
        throw new IllegalArgumentException("complete action must not define agentId or role");
      }
    } else {
      agentId = AgentModelSupport.requireText(agentId, "agentId");
      role = Objects.requireNonNull(role, "role must not be null");
    }
  }

  /**
   * Creates one completion action.
   *
   * @param preferred whether completion is the preferred default
   * @param reason short rationale shown to the caller
   * @return validated completion action
   */
  public static AgentRunAvailableAction complete(boolean preferred, String reason) {
    return new AgentRunAvailableAction(null, null, preferred, true, reason);
  }
}
