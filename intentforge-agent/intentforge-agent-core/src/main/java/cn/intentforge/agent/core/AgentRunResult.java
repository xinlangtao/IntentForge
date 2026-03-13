package cn.intentforge.agent.core;

import cn.intentforge.tool.core.model.ToolCallResult;
import java.util.List;

/**
 * Final execution result returned by the agent gateway.
 *
 * @param contextPack resolved execution context
 * @param route selected route
 * @param plan final plan snapshot
 * @param decisions ordered stage decisions
 * @param artifacts ordered artifacts
 * @param toolCalls ordered tool call results
 * @param summary short final summary
 */
public record AgentRunResult(
    ContextPack contextPack,
    AgentRoute route,
    Plan plan,
    List<Decision> decisions,
    List<Artifact> artifacts,
    List<ToolCallResult> toolCalls,
    String summary
) {
  /**
   * Creates a validated run result.
   */
  public AgentRunResult {
    contextPack = java.util.Objects.requireNonNull(contextPack, "contextPack must not be null");
    route = java.util.Objects.requireNonNull(route, "route must not be null");
    decisions = AgentModelSupport.immutableList(decisions, "decisions");
    artifacts = AgentModelSupport.immutableList(artifacts, "artifacts");
    toolCalls = AgentModelSupport.immutableList(toolCalls, "toolCalls");
    summary = cn.intentforge.common.util.ValidationSupport.requireText(summary, "summary");
  }
}
