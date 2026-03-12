package cn.intentforge.agent.core;

import cn.intentforge.tool.core.model.ToolCallResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable accumulated state passed across routed agent stages.
 *
 * @param plan latest generated plan, if any
 * @param decisions ordered stage decisions
 * @param artifacts ordered emitted artifacts
 * @param toolCalls ordered tool call results
 * @param messages ordered user or runtime messages accumulated across turns
 */
public record AgentExecutionState(
    Plan plan,
    List<Decision> decisions,
    List<Artifact> artifacts,
    List<ToolCallResult> toolCalls,
    List<AgentRunMessage> messages
) {
  /**
   * Creates a validated state snapshot.
   */
  public AgentExecutionState {
    decisions = AgentModelSupport.immutableList(decisions, "decisions");
    artifacts = AgentModelSupport.immutableList(artifacts, "artifacts");
    toolCalls = AgentModelSupport.immutableList(toolCalls, "toolCalls");
    messages = AgentModelSupport.immutableList(messages, "messages");
  }

  /**
   * Returns an empty execution state.
   *
   * @return empty state
   */
  public static AgentExecutionState empty() {
    return new AgentExecutionState(null, List.of(), List.of(), List.of(), List.of());
  }

  /**
   * Merges one step result into the current state.
   *
   * @param stepResult step result to append
   * @return merged immutable state
   */
  public AgentExecutionState merge(AgentStepResult stepResult) {
    AgentStepResult nonNullResult = java.util.Objects.requireNonNull(stepResult, "stepResult must not be null");
    List<Decision> mergedDecisions = new ArrayList<>(decisions);
    mergedDecisions.add(nonNullResult.decision());
    List<Artifact> mergedArtifacts = new ArrayList<>(artifacts);
    mergedArtifacts.addAll(nonNullResult.artifacts());
    List<ToolCallResult> mergedToolCalls = new ArrayList<>(toolCalls);
    mergedToolCalls.addAll(nonNullResult.toolCalls());
    return new AgentExecutionState(
        nonNullResult.plan() == null ? plan : nonNullResult.plan(),
        mergedDecisions,
        mergedArtifacts,
        mergedToolCalls,
        messages);
  }

  /**
   * Appends one run message without dropping accumulated execution results.
   *
   * @param message run message to append
   * @return merged immutable state
   */
  public AgentExecutionState appendMessage(AgentRunMessage message) {
    AgentRunMessage nonNullMessage = java.util.Objects.requireNonNull(message, "message must not be null");
    List<AgentRunMessage> mergedMessages = new ArrayList<>(messages);
    mergedMessages.add(nonNullMessage);
    return new AgentExecutionState(plan, decisions, artifacts, toolCalls, mergedMessages);
  }
}
