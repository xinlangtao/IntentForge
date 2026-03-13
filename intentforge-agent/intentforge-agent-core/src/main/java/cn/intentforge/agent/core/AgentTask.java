package cn.intentforge.agent.core;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable request used to execute one routed coding task.
 *
 * @param id stable task identifier
 * @param sessionId target session identifier
 * @param spaceId optional explicit space identifier; when absent the session space is used
 * @param workspaceRoot workspace root used for tool execution
 * @param mode requested execution depth
 * @param intent user intent or objective
 * @param targetAgentId optional explicit target agent identifier
 * @param metadata task metadata
 */
public record AgentTask(
    String id,
    String sessionId,
    String spaceId,
    Path workspaceRoot,
    TaskMode mode,
    String intent,
    String targetAgentId,
    Map<String, String> metadata
) {
  /**
   * Creates a validated task request.
   */
  public AgentTask {
    id = cn.intentforge.common.util.ValidationSupport.requireText(id, "id");
    sessionId = cn.intentforge.common.util.ValidationSupport.requireText(sessionId, "sessionId");
    spaceId = cn.intentforge.common.util.ValidationSupport.normalize(spaceId);
    workspaceRoot = AgentModelSupport.normalizeWorkspace(workspaceRoot);
    mode = Objects.requireNonNullElse(mode, TaskMode.FULL);
    intent = cn.intentforge.common.util.ValidationSupport.requireText(intent, "intent");
    targetAgentId = cn.intentforge.common.util.ValidationSupport.normalize(targetAgentId);
    metadata = AgentModelSupport.immutableMetadata(metadata);
  }
}
