package cn.intentforge.governance.agent;

import cn.intentforge.agent.core.AgentDescriptor;
import cn.intentforge.agent.core.AgentExecutionException;
import cn.intentforge.agent.core.AgentExecutionState;
import cn.intentforge.agent.core.AgentExecutor;
import cn.intentforge.agent.core.AgentRole;
import cn.intentforge.agent.core.AgentRunResult;
import cn.intentforge.agent.core.AgentStepResult;
import cn.intentforge.agent.core.AgentTask;
import cn.intentforge.agent.core.Artifact;
import cn.intentforge.agent.core.ContextPack;
import cn.intentforge.agent.core.Decision;
import cn.intentforge.agent.core.Plan;
import cn.intentforge.agent.core.PlanStep;
import cn.intentforge.agent.core.TaskMode;
import cn.intentforge.model.catalog.ModelCapability;
import cn.intentforge.model.catalog.ModelDescriptor;
import cn.intentforge.model.catalog.ModelType;
import cn.intentforge.model.local.registry.InMemoryModelManager;
import cn.intentforge.model.provider.ModelProvider;
import cn.intentforge.model.provider.ModelProviderDescriptor;
import cn.intentforge.model.provider.ModelProviderType;
import cn.intentforge.model.provider.local.registry.InMemoryModelProviderRegistry;
import cn.intentforge.prompt.local.registry.InMemoryPromptManager;
import cn.intentforge.prompt.model.PromptDefinition;
import cn.intentforge.prompt.model.PromptKind;
import cn.intentforge.session.local.registry.InMemorySessionManager;
import cn.intentforge.session.model.SessionDraft;
import cn.intentforge.space.ResolvedSpaceProfile;
import cn.intentforge.space.SpaceResolver;
import cn.intentforge.space.SpaceType;
import cn.intentforge.tool.core.gateway.ToolGateway;
import cn.intentforge.tool.core.model.ToolCallRequest;
import cn.intentforge.tool.core.model.ToolCallResult;
import cn.intentforge.tool.core.model.ToolDefinition;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultAgentGatewayTest {
  @Test
  void shouldResolveContextAndExecuteRoute() throws Exception {
    InMemorySessionManager sessionManager = new InMemorySessionManager();
    sessionManager.create(new SessionDraft("session-1", "Coding", "application-alpha", Map.of()));

    InMemoryPromptManager promptManager = new InMemoryPromptManager();
    promptManager.register(new PromptDefinition(
        "prompt-1",
        "v1",
        "Planner Prompt",
        "Native planner prompt",
        PromptKind.SYSTEM,
        List.of(),
        List.of("coding"),
        "Plan the coding task",
        Map.of()));

    InMemoryModelManager modelManager = new InMemoryModelManager();
    modelManager.register(new ModelDescriptor(
        "model-1",
        "provider-1",
        "Coding Model",
        "Native coding model",
        ModelType.CHAT,
        List.of(ModelCapability.CHAT, ModelCapability.REASONING),
        32000,
        true,
        Map.of()));

    InMemoryModelProviderRegistry providerRegistry = new InMemoryModelProviderRegistry();
    providerRegistry.register(new ModelProvider() {
      @Override
      public ModelProviderDescriptor descriptor() {
        return new ModelProviderDescriptor(
            "provider-1",
            "Provider",
            "Native provider",
            ModelProviderType.CUSTOM,
            "native://provider",
            List.of(ModelCapability.CHAT),
            Map.of());
      }
    });

    SpaceResolver spaceResolver = spaceId -> new ResolvedSpaceProfile(
        spaceId,
        SpaceType.APPLICATION,
        List.of("company-root", "project-alpha", "product-alpha", spaceId),
        List.of(),
        List.of("intentforge.native.planner", "intentforge.native.coder", "intentforge.native.reviewer"),
        List.of("prompt-1"),
        List.of("intentforge.fs.list"),
        List.of("model-1"),
        List.of("provider-1"),
        List.of(),
        Map.of("review.level", "mvp"));

    AtomicReference<ContextPack> observedContext = new AtomicReference<>();
    DefaultAgentGateway gateway = new DefaultAgentGateway(
        sessionManager,
        spaceResolver,
        promptManager,
        modelManager,
        providerRegistry,
        new StubToolGateway(List.of(
            new ToolDefinition("intentforge.fs.list", "list", Map.of(), false),
            new ToolDefinition("intentforge.shell.exec", "exec", Map.of(), true))),
        new StageRoutingAgentRouter(),
        List.of(
            new StubExecutor("intentforge.native.planner", AgentRole.PLANNER, observedContext, true),
            new StubExecutor("intentforge.native.coder", AgentRole.CODER, observedContext, false),
            new StubExecutor("intentforge.native.reviewer", AgentRole.REVIEWER, observedContext, false)));

    AgentRunResult result = gateway.execute(new AgentTask(
        "task-1",
        "session-1",
        null,
        Files.createTempDirectory("gateway-workspace"),
        TaskMode.FULL,
        "Implement gateway flow",
        null,
        Map.of("ticket", "IF-1")));

    Assertions.assertNotNull(observedContext.get());
    Assertions.assertEquals(List.of("prompt-1"), observedContext.get().prompts().stream().map(PromptDefinition::id).toList());
    Assertions.assertEquals(List.of("model-1"), observedContext.get().models().stream().map(ModelDescriptor::id).toList());
    Assertions.assertEquals(
        List.of("provider-1"),
        observedContext.get().modelProviders().stream().map(ModelProviderDescriptor::id).toList());
    Assertions.assertEquals(List.of("intentforge.fs.list"), observedContext.get().tools().stream().map(ToolDefinition::id).toList());
    Assertions.assertEquals(3, result.route().steps().size());
    Assertions.assertEquals(3, result.decisions().size());
    Assertions.assertEquals(2, result.artifacts().size());
    Assertions.assertEquals("intentforge.native.reviewer completed", result.summary());
  }

  @Test
  void shouldRejectWhenSessionIsMissing() throws Exception {
    DefaultAgentGateway gateway = new DefaultAgentGateway(
        new InMemorySessionManager(),
        spaceId -> new ResolvedSpaceProfile(
            spaceId,
            SpaceType.APPLICATION,
            List.of(spaceId),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            Map.of()),
        new InMemoryPromptManager(),
        new InMemoryModelManager(),
        new InMemoryModelProviderRegistry(),
        new StubToolGateway(List.of()),
        new StageRoutingAgentRouter(),
        List.of());

    AgentExecutionException exception = Assertions.assertThrows(AgentExecutionException.class, () -> gateway.execute(
        new AgentTask(
            "task-1",
            "missing-session",
            "application-alpha",
            Files.createTempDirectory("gateway-missing"),
            TaskMode.PLAN_ONLY,
            "Plan work",
            null,
            Map.of())));
    Assertions.assertTrue(exception.getMessage().contains("missing-session"));
  }

  private static final class StubExecutor implements AgentExecutor {
    private final AgentDescriptor descriptor;
    private final AtomicReference<ContextPack> observedContext;
    private final boolean returnsPlan;

    private StubExecutor(
        String id,
        AgentRole role,
        AtomicReference<ContextPack> observedContext,
        boolean returnsPlan
    ) {
      this.descriptor = new AgentDescriptor(id, role, role.name(), role.name());
      this.observedContext = observedContext;
      this.returnsPlan = returnsPlan;
    }

    @Override
    public AgentDescriptor descriptor() {
      return descriptor;
    }

    @Override
    public AgentStepResult execute(ContextPack contextPack, AgentExecutionState state) {
      observedContext.compareAndSet(null, contextPack);
      Plan plan = returnsPlan
          ? new Plan(
              "Native plan",
              List.of(new PlanStep("step-1", "Inspect", "Inspect workspace", "intentforge.fs.list", true)),
              Map.of("source", descriptor.id()))
          : null;
      List<Artifact> artifacts = descriptor.role() == AgentRole.PLANNER
          ? List.of()
          : List.of(new Artifact(descriptor.id() + ".md", "text/markdown", descriptor.id() + " artifact", Map.of()));
      return new AgentStepResult(
          plan,
          new Decision(descriptor.id(), descriptor.role(), descriptor.id() + " completed", Map.of()),
          artifacts,
          List.of());
    }
  }

  private static final class StubToolGateway implements ToolGateway {
    private final List<ToolDefinition> toolDefinitions;

    private StubToolGateway(List<ToolDefinition> toolDefinitions) {
      this.toolDefinitions = toolDefinitions;
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
      return ToolCallResult.success("not-used");
    }

    @Override
    public List<ToolDefinition> listTools() {
      return toolDefinitions;
    }
  }
}
