package cn.intentforge.agent.nativejava;

import cn.intentforge.agent.core.AgentDescriptor;
import cn.intentforge.agent.core.AgentExecutionException;
import cn.intentforge.agent.core.AgentExecutionState;
import cn.intentforge.agent.core.AgentExecutor;
import cn.intentforge.agent.core.AgentRole;
import cn.intentforge.agent.core.AgentStepResult;
import cn.intentforge.agent.core.Artifact;
import cn.intentforge.agent.core.ContextPack;
import cn.intentforge.agent.core.Decision;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic reviewer for the native Java coding-agent MVP.
 */
public final class NativeReviewerAgent implements AgentExecutor {
  private static final AgentDescriptor DESCRIPTOR = new AgentDescriptor(
      "intentforge.native.reviewer",
      AgentRole.REVIEWER,
      "Native Reviewer",
      "Produces a minimal review artifact from the accumulated MVP execution state.");

  /**
   * Returns public metadata for this executor.
   *
   * @return executor descriptor
   */
  @Override
  public AgentDescriptor descriptor() {
    return DESCRIPTOR;
  }

  /**
   * Executes the review stage.
   *
   * @param contextPack resolved execution context
   * @param state accumulated execution state before this stage
   * @return stage result
   */
  @Override
  public AgentStepResult execute(ContextPack contextPack, AgentExecutionState state) {
    Objects.requireNonNull(contextPack, "contextPack must not be null");
    AgentExecutionState nonNullState = Objects.requireNonNull(state, "state must not be null");
    if (nonNullState.plan() == null) {
      throw new AgentExecutionException("plan must be available before review");
    }
    String content = """
        # Native Review
        summary: %s
        artifacts: %d
        tool calls: %d
        feedback messages: %d
        """.formatted(
        nonNullState.plan().summary(),
        nonNullState.artifacts().size(),
        nonNullState.toolCalls().size(),
        nonNullState.messages().size());
    return new AgentStepResult(
        null,
        new Decision(descriptor().id(), descriptor().role(), descriptor().id() + " completed", Map.of("stage", "review")),
        List.of(new Artifact("native-review.md", "text/markdown", content, Map.of("agentId", descriptor().id()))),
        List.of());
  }
}
