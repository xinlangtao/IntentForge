package cn.intentforge.agent.core;

import java.util.Objects;

/**
 * User-supplied checkpoint transition that selects the next action for one paused run.
 *
 * @param feedback optional user feedback captured for the next turn
 * @param nextAgentId optional explicit agent identifier chosen by the user
 * @param nextRole optional role chosen by the user when the concrete agent should be resolved by governance
 * @param complete whether the user wants to finish the run at the current checkpoint
 */
public record AgentRunTransition(
    String feedback,
    String nextAgentId,
    AgentRole nextRole,
    boolean complete
) {
  /**
   * Creates one validated transition request.
   */
  public AgentRunTransition {
    feedback = AgentModelSupport.normalize(feedback);
    nextAgentId = AgentModelSupport.normalize(nextAgentId);
    if (complete) {
      if (nextAgentId != null || nextRole != null) {
        throw new IllegalArgumentException("complete transition must not define nextAgentId or nextRole");
      }
    } else if (nextAgentId == null && nextRole == null) {
      throw new IllegalArgumentException("one of nextRole, nextAgentId, or complete must be provided");
    } else {
      nextRole = nextRole == null ? null : Objects.requireNonNull(nextRole, "nextRole must not be null");
    }
  }
}
