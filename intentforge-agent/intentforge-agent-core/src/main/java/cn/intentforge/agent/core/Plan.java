package cn.intentforge.agent.core;

import java.util.List;
import java.util.Map;

/**
 * Execution plan generated for one coding task.
 *
 * @param summary short plan summary
 * @param steps ordered plan steps
 * @param metadata plan metadata such as selected prompt/model/provider
 */
public record Plan(
    String summary,
    List<PlanStep> steps,
    Map<String, String> metadata
) {
  /**
   * Creates a validated plan.
   */
  public Plan {
    summary = cn.intentforge.common.util.ValidationSupport.requireText(summary, "summary");
    steps = AgentModelSupport.immutableList(steps, "steps");
    if (steps.isEmpty()) {
      throw new IllegalArgumentException("steps must not be empty");
    }
    metadata = AgentModelSupport.immutableMetadata(metadata);
  }
}
