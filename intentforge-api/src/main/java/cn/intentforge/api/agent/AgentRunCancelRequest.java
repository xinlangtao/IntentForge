package cn.intentforge.api.agent;

import cn.intentforge.api.util.ApiModelSupport;

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
    reason = ApiModelSupport.requireText(reason, "reason");
  }
}
