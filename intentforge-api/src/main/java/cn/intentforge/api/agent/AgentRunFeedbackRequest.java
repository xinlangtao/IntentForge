package cn.intentforge.api.agent;

/**
 * HTTP request used to resume one paused run with user feedback.
 *
 * @param content user feedback content
 */
public record AgentRunFeedbackRequest(String content) {
  /**
   * Creates a validated feedback request.
   */
  public AgentRunFeedbackRequest {
    content = ApiModelSupport.requireText(content, "content");
  }
}
