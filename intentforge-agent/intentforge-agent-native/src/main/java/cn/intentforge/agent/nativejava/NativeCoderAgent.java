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
import cn.intentforge.tool.core.gateway.ToolGateway;
import cn.intentforge.tool.core.model.ToolCallRequest;
import cn.intentforge.tool.core.model.ToolCallResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic coder for the native Java coding-agent MVP.
 */
public final class NativeCoderAgent implements AgentExecutor {
  private static final AgentDescriptor DESCRIPTOR = new AgentDescriptor(
      "intentforge.native.coder",
      AgentRole.CODER,
      "Native Coder",
      "Runs a minimal tool-assisted implementation pass for the MVP flow.");

  private final ToolGateway toolGateway;

  /**
   * Creates the coder with a tool gateway.
   *
   * @param toolGateway tool gateway used for workspace inspection
   */
  public NativeCoderAgent(ToolGateway toolGateway) {
    this.toolGateway = Objects.requireNonNull(toolGateway, "toolGateway must not be null");
  }

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
   * Executes the coding stage.
   *
   * @param contextPack resolved execution context
   * @param state accumulated execution state before this stage
   * @return stage result
   */
  @Override
  public AgentStepResult execute(ContextPack contextPack, AgentExecutionState state) {
    ContextPack nonNullContextPack = Objects.requireNonNull(contextPack, "contextPack must not be null");
    AgentExecutionState nonNullState = Objects.requireNonNull(state, "state must not be null");
    if (nonNullState.plan() == null) {
      throw new AgentExecutionException("plan must be available before coding");
    }

    List<ToolCallResult> toolCalls = new ArrayList<>();
    if (NativeAgentSupport.hasTool(nonNullContextPack, "intentforge.fs.list")) {
      toolCalls.add(toolGateway.execute(new ToolCallRequest(
          "intentforge.fs.list",
          Map.of("path", "."),
          nonNullContextPack.toolExecutionContext())));
    }
    if (NativeAgentSupport.hasTool(nonNullContextPack, "intentforge.runtime.environment.read")) {
      toolCalls.add(toolGateway.execute(new ToolCallRequest(
          "intentforge.runtime.environment.read",
          Map.of(),
          nonNullContextPack.toolExecutionContext())));
    }

    String latestFeedback = nonNullState.messages().isEmpty() ? "none" : nonNullState.messages().getLast().content();

    String content = """
        # Native Coder Output
        task: %s
        prompt: %s
        model: %s
        provider: %s
        workspace: %s
        tool calls: %d
        latest feedback: %s
        """.formatted(
        nonNullContextPack.task().intent(),
        NativeAgentSupport.firstPromptId(nonNullContextPack),
        NativeAgentSupport.firstModelId(nonNullContextPack),
        NativeAgentSupport.firstProviderId(nonNullContextPack),
        nonNullContextPack.task().workspaceRoot(),
        toolCalls.size(),
        latestFeedback);
    return new AgentStepResult(
        null,
        new Decision(descriptor().id(), descriptor().role(), descriptor().id() + " completed", Map.of("stage", "code")),
        List.of(new Artifact("native-coder-output.md", "text/markdown", content, Map.of("agentId", descriptor().id()))),
        toolCalls);
  }
}
