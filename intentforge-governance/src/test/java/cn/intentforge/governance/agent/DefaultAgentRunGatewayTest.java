package cn.intentforge.governance.agent;

import cn.intentforge.agent.core.AgentDescriptor;
import cn.intentforge.agent.core.AgentExecutionException;
import cn.intentforge.agent.core.AgentExecutionState;
import cn.intentforge.agent.core.AgentExecutor;
import cn.intentforge.agent.core.AgentRole;
import cn.intentforge.agent.core.AgentRunEvent;
import cn.intentforge.agent.core.AgentRunEventType;
import cn.intentforge.agent.core.AgentRunGateway;
import cn.intentforge.agent.core.AgentRunMessage;
import cn.intentforge.agent.core.AgentRunSnapshot;
import cn.intentforge.agent.core.AgentRunStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultAgentRunGatewayTest {
  @Test
  void shouldStartRunEmitEventsAndPauseForUserAfterPlanner() throws Exception {
    AgentRunGateway gateway = createGateway();
    List<AgentRunEvent> observedEvents = new ArrayList<>();

    AgentRunSnapshot snapshot = gateway.start(new AgentTask(
        "task-1",
        "session-1",
        null,
        Files.createTempDirectory("agent-run-start"),
        TaskMode.FULL,
        "Implement event-driven run model",
        null,
        Map.of("story", "IF-201")), observedEvents::add);

    Assertions.assertEquals(AgentRunStatus.AWAITING_USER, snapshot.status());
    Assertions.assertEquals(1, snapshot.nextStepIndex());
    Assertions.assertEquals(1, snapshot.state().decisions().size());
    Assertions.assertNotNull(snapshot.state().plan());
    Assertions.assertEquals(snapshot.events(), observedEvents);
    Assertions.assertEquals(
        List.of(
            AgentRunEventType.RUN_CREATED,
            AgentRunEventType.CONTEXT_RESOLVED,
            AgentRunEventType.ROUTE_SELECTED,
            AgentRunEventType.STAGE_STARTED,
            AgentRunEventType.STAGE_COMPLETED,
            AgentRunEventType.AWAITING_USER),
        snapshot.events().stream().map(AgentRunEvent::type).toList());
  }

  @Test
  void shouldResumePausedRunWithFeedbackAndCompleteAcrossStages() throws Exception {
    AgentRunGateway gateway = createGateway();

    AgentRunSnapshot pausedAfterPlanner = gateway.start(new AgentTask(
        "task-1",
        "session-1",
        null,
        Files.createTempDirectory("agent-run-resume"),
        TaskMode.FULL,
        "Implement event-driven run model",
        null,
        Map.of()), event -> {
    });
    AgentRunSnapshot pausedAfterCoder = gateway.resume(pausedAfterPlanner.runId(), "Need stronger validation", event -> {
    });

    Assertions.assertEquals(AgentRunStatus.AWAITING_USER, pausedAfterCoder.status());
    Assertions.assertEquals(2, pausedAfterCoder.nextStepIndex());
    Assertions.assertTrue(pausedAfterCoder.state().messages().stream()
        .map(AgentRunMessage::content)
        .toList()
        .contains("Need stronger validation"));
    Assertions.assertTrue(pausedAfterCoder.events().stream().anyMatch(event ->
        event.type() == AgentRunEventType.USER_FEEDBACK_RECEIVED));

    AgentRunSnapshot completed = gateway.resume(pausedAfterPlanner.runId(), "Focus review on edge cases", event -> {
    });

    Assertions.assertEquals(AgentRunStatus.COMPLETED, completed.status());
    Assertions.assertEquals(3, completed.state().decisions().size());
    Assertions.assertEquals(2, completed.state().messages().size());
    Assertions.assertEquals(AgentRunEventType.RUN_COMPLETED, completed.events().getLast().type());
  }

  @Test
  void shouldCancelPausedRun() throws Exception {
    AgentRunGateway gateway = createGateway();
    AgentRunSnapshot paused = gateway.start(new AgentTask(
        "task-1",
        "session-1",
        null,
        Files.createTempDirectory("agent-run-cancel"),
        TaskMode.FULL,
        "Implement event-driven run model",
        null,
        Map.of()), event -> {
    });

    AgentRunSnapshot cancelled = gateway.cancel(paused.runId(), "User stopped the run", event -> {
    });

    Assertions.assertEquals(AgentRunStatus.CANCELLED, cancelled.status());
    Assertions.assertEquals(AgentRunEventType.RUN_CANCELLED, cancelled.events().getLast().type());
    Assertions.assertTrue(cancelled.awaitingReason().contains("User stopped the run"));
  }

  @Test
  void shouldRejectResumeWhenRunIsNotAwaitingUser() throws Exception {
    AgentRunGateway gateway = createGateway();
    AgentRunSnapshot completed = gateway.start(new AgentTask(
        "task-1",
        "session-1",
        null,
        Files.createTempDirectory("agent-run-invalid"),
        TaskMode.PLAN_ONLY,
        "Plan only",
        null,
        Map.of()), event -> {
    });
    Assertions.assertEquals(AgentRunStatus.COMPLETED, completed.status());

    AgentExecutionException exception = Assertions.assertThrows(
        AgentExecutionException.class,
        () -> gateway.resume(completed.runId(), "continue", event -> {
        }));
    Assertions.assertTrue(exception.getMessage().contains("awaiting"));
  }

  private static AgentRunGateway createGateway() {
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

    return new DefaultAgentRunGateway(
        sessionManager,
        spaceResolver,
        promptManager,
        modelManager,
        providerRegistry,
        new StubToolGateway(List.of(new ToolDefinition("intentforge.fs.list", "list", Map.of(), false))),
        new StageRoutingAgentRouter(),
        List.of(
            new StubExecutor("intentforge.native.planner", AgentRole.PLANNER, true),
            new StubExecutor("intentforge.native.coder", AgentRole.CODER, false),
            new StubExecutor("intentforge.native.reviewer", AgentRole.REVIEWER, false)));
  }

  private static final class StubExecutor implements AgentExecutor {
    private final AgentDescriptor descriptor;
    private final boolean returnsPlan;

    private StubExecutor(String id, AgentRole role, boolean returnsPlan) {
      this.descriptor = new AgentDescriptor(id, role, role.name(), role.name());
      this.returnsPlan = returnsPlan;
    }

    @Override
    public AgentDescriptor descriptor() {
      return descriptor;
    }

    @Override
    public AgentStepResult execute(ContextPack contextPack, AgentExecutionState state) {
      Plan plan = returnsPlan
          ? new Plan(
              "Native plan",
              List.of(new PlanStep("step-1", "Inspect", "Inspect workspace", "intentforge.fs.list", true)),
              Map.of("source", descriptor.id()))
          : null;
      String feedback = state.messages().isEmpty() ? "none" : state.messages().getLast().content();
      List<Artifact> artifacts = descriptor.role() == AgentRole.PLANNER
          ? List.of()
          : List.of(new Artifact(
              descriptor.id() + ".md",
              "text/markdown",
              descriptor.id() + " artifact with feedback: " + feedback,
              Map.of()));
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
