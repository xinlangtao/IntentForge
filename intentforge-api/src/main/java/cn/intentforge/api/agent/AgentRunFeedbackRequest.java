package cn.intentforge.api.agent;

import cn.intentforge.api.util.ApiModelSupport;

/**
 * HTTP request used to resume one paused run with user feedback.
 *
 * @param content optional user feedback content
 * @param nextRole optional next role selected by the user
 * @param nextAgentId optional next agent identifier selected by the user
 * @param complete whether the user wants to complete the run at the current checkpoint
 */
public record AgentRunFeedbackRequest(
    String content,
    String nextRole,
    String nextAgentId,
    boolean complete
) {
  /**
   * Creates a validated feedback request.
   */
  public AgentRunFeedbackRequest {
    content = ApiModelSupport.normalize(content);
    nextRole = ApiModelSupport.normalize(nextRole);
    nextAgentId = ApiModelSupport.normalize(nextAgentId);
    if (complete) {
      if (nextRole != null || nextAgentId != null) {
        throw new IllegalArgumentException("complete request must not define nextRole or nextAgentId");
      }
    } else if (nextRole == null && nextAgentId == null) {
      throw new IllegalArgumentException("one of nextRole, nextAgentId, or complete must be provided");
    }
  }
}
