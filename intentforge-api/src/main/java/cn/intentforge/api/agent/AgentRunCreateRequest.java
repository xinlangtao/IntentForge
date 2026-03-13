package cn.intentforge.api.agent;

import cn.intentforge.api.util.ApiModelSupport;

import java.util.Map;

/**
 * HTTP request used to create and start one agent run.
 *
 * @param taskId stable task identifier
 * @param sessionId optional target session identifier; when absent the server creates one session for the supplied space
 * @param spaceId optional explicit space identifier
 * @param workspaceRoot workspace root path string
 * @param mode requested execution mode
 * @param intent user intent for the run
 * @param targetAgentId optional explicit target agent identifier
 * @param metadata optional task metadata
 */
public record AgentRunCreateRequest(
    String taskId,
    String sessionId,
    String spaceId,
    String workspaceRoot,
    String mode,
    String intent,
    String targetAgentId,
    Map<String, String> metadata
) {
  /**
   * Creates a validated create request.
   */
  public AgentRunCreateRequest {
    taskId = ApiModelSupport.requireText(taskId, "taskId");
    String normalizedSessionId = ApiModelSupport.normalize(sessionId);
    if (sessionId != null && normalizedSessionId == null) {
      throw new IllegalArgumentException("sessionId must not be blank");
    }
    sessionId = normalizedSessionId;
    spaceId = ApiModelSupport.normalize(spaceId);
    workspaceRoot = ApiModelSupport.requireText(workspaceRoot, "workspaceRoot");
    mode = ApiModelSupport.requireText(mode, "mode");
    intent = ApiModelSupport.requireText(intent, "intent");
    targetAgentId = ApiModelSupport.normalize(targetAgentId);
    metadata = ApiModelSupport.immutableStringMap(metadata, "metadata");
  }
}
