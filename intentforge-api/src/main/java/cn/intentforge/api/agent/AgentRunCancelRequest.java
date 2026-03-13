package cn.intentforge.api.agent;


/**
 * HTTP request used to cancel one run.
 *
 * @param reason human-readable cancellation reason
 */
public record AgentRunCancelRequest(String reason) {
  /**
   * Creates a validated cancel request.
   */
  public AgentRunCancelRequest {
    reason = cn.intentforge.common.util.ValidationSupport.requireText(reason, "reason");
  }
}
