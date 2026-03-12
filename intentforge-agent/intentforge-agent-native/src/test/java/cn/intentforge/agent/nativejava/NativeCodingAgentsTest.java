package cn.intentforge.agent.nativejava;

import cn.intentforge.agent.core.AgentExecutionException;
import cn.intentforge.agent.core.AgentExecutionState;
import cn.intentforge.agent.core.AgentStepResult;
import cn.intentforge.agent.core.AgentTask;
import cn.intentforge.agent.core.ContextPack;
import cn.intentforge.agent.core.TaskMode;
import cn.intentforge.model.catalog.ModelCapability;
import cn.intentforge.model.catalog.ModelDescriptor;
import cn.intentforge.model.catalog.ModelType;
import cn.intentforge.model.provider.ModelProviderDescriptor;
import cn.intentforge.model.provider.ModelProviderType;
import cn.intentforge.prompt.model.PromptDefinition;
import cn.intentforge.prompt.model.PromptKind;
import cn.intentforge.session.model.Session;
import cn.intentforge.session.model.SessionStatus;
import cn.intentforge.space.ResolvedSpaceProfile;
import cn.intentforge.space.SpaceType;
import cn.intentforge.tool.core.gateway.ToolGateway;
import cn.intentforge.tool.core.model.ToolCallRequest;
import cn.intentforge.tool.core.model.ToolCallResult;
import cn.intentforge.tool.core.model.ToolDefinition;
import cn.intentforge.tool.core.model.ToolExecutionContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NativeCodingAgentsTest {
  @Test
  void shouldProducePlanCodeAndReviewArtifacts() throws Exception {
    Path workspace = Files.createTempDirectory("native-agent");
    Files.writeString(workspace.resolve("README.md"), "intentforge");
    ContextPack contextPack = contextPack(workspace);

    NativePlannerAgent planner = new NativePlannerAgent();
    NativeCoderAgent coder = new NativeCoderAgent(new StubToolGateway());
    NativeReviewerAgent reviewer = new NativeReviewerAgent();

    AgentStepResult planning = planner.execute(contextPack, AgentExecutionState.empty());
    Assertions.assertNotNull(planning.plan());
    Assertions.assertEquals(3, planning.plan().steps().size());

    AgentExecutionState codingState = AgentExecutionState.empty().merge(planning);
    AgentStepResult coding = coder.execute(contextPack, codingState);
    Assertions.assertEquals(2, coding.toolCalls().size());
    Assertions.assertTrue(coding.artifacts().getFirst().content().contains("prompt-1"));
    Assertions.assertTrue(coding.artifacts().getFirst().content().contains("model-1"));
    Assertions.assertTrue(coding.artifacts().getFirst().content().contains("provider-1"));

    AgentExecutionState reviewState = codingState.merge(coding);
    AgentStepResult review = reviewer.execute(contextPack, reviewState);
    Assertions.assertTrue(review.artifacts().getFirst().content().contains("tool calls: 2"));
  }

  @Test
  void shouldRejectCodingWhenPlanIsMissing() throws Exception {
    NativeCoderAgent coder = new NativeCoderAgent(new StubToolGateway());

    AgentExecutionException exception = Assertions.assertThrows(
        AgentExecutionException.class,
        () -> coder.execute(contextPack(Files.createTempDirectory("native-agent-missing")), AgentExecutionState.empty()));
    Assertions.assertTrue(exception.getMessage().contains("plan"));
  }

  private static ContextPack contextPack(Path workspace) {
    Instant now = Instant.parse("2026-03-12T10:00:00Z");
    AgentTask task = new AgentTask(
        "task-1",
        "session-1",
        "application-alpha",
        workspace,
        TaskMode.FULL,
        "Implement native coding agent",
        null,
        Map.of());
    return new ContextPack(
        task,
        new Session("session-1", "Coding", "application-alpha", SessionStatus.ACTIVE, List.of(), Map.of(), now, now),
        new ResolvedSpaceProfile(
            "application-alpha",
            SpaceType.APPLICATION,
            List.of("company-root", "project-alpha", "product-alpha", "application-alpha"),
            List.of(),
            List.of("intentforge.native.planner", "intentforge.native.coder", "intentforge.native.reviewer"),
            List.of("prompt-1"),
            List.of("intentforge.fs.list", "intentforge.runtime.environment.read"),
            List.of("model-1"),
            List.of("provider-1"),
            List.of(),
            Map.of()),
        List.of(new PromptDefinition(
            "prompt-1",
            "v1",
            "Planner Prompt",
            "prompt",
            PromptKind.SYSTEM,
            List.of(),
            List.of("coding"),
            "Plan it",
            Map.of())),
        List.of(new ModelDescriptor(
            "model-1",
            "provider-1",
            "Coding Model",
            "model",
            ModelType.CHAT,
            List.of(ModelCapability.CHAT, ModelCapability.REASONING),
            32000,
            true,
            Map.of())),
        List.of(new ModelProviderDescriptor(
            "provider-1",
            "Provider",
            "provider",
            ModelProviderType.CUSTOM,
            "native://provider",
            List.of(ModelCapability.CHAT),
            Map.of())),
        List.of(
            new ToolDefinition("intentforge.fs.list", "list", Map.of(), false),
            new ToolDefinition("intentforge.runtime.environment.read", "env", Map.of(), false)),
        ToolExecutionContext.create(workspace));
  }

  private static final class StubToolGateway implements ToolGateway {
    @Override
    public ToolCallResult execute(ToolCallRequest request) {
      return switch (request.toolId()) {
        case "intentforge.fs.list" -> ToolCallResult.success(
            "listed",
            List.of("README.md"),
            Map.of("path", request.parameters().get("path")));
        case "intentforge.runtime.environment.read" -> ToolCallResult.success(
            "environment",
            Map.of("os", "macos", "shell", "zsh"),
            Map.of());
        default -> ToolCallResult.error("TOOL_NOT_FOUND", request.toolId());
      };
    }

    @Override
    public List<ToolDefinition> listTools() {
      return List.of(
          new ToolDefinition("intentforge.fs.list", "list", Map.of(), false),
          new ToolDefinition("intentforge.runtime.environment.read", "env", Map.of(), false));
    }
  }
}
