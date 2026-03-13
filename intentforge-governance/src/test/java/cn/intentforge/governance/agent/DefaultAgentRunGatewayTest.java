package cn.intentforge.governance.agent;

import cn.intentforge.agent.core.AgentDescriptor;
import cn.intentforge.agent.core.AgentExecutionException;
import cn.intentforge.agent.core.AgentExecutionState;
import cn.intentforge.agent.core.AgentExecutor;
import cn.intentforge.agent.core.AgentRole;
import cn.intentforge.agent.core.AgentRunAvailableAction;
import cn.intentforge.agent.core.AgentRunEvent;
import cn.intentforge.agent.core.AgentRunEventType;
import cn.intentforge.agent.core.AgentRunGateway;
import cn.intentforge.agent.core.AgentRunMessage;
import cn.intentforge.agent.core.AgentRunSnapshot;
import cn.intentforge.agent.core.AgentRunStatus;
import cn.intentforge.agent.core.AgentRunTransition;
import cn.intentforge.agent.core.AgentStepResult;
import cn.intentforge.agent.core.AgentTask;
import cn.intentforge.agent.core.Artifact;
import cn.intentforge.agent.core.ContextPack;
import cn.intentforge.agent.core.Decision;
import cn.intentforge.agent.core.Plan;
import cn.intentforge.agent.core.PlanStep;
import cn.intentforge.agent.core.TaskMode;
import cn.intentforge.config.ResolvedRuntimeSelection;
import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.config.RuntimeImplementationDescriptor;
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
    Assertions.assertEquals(1, snapshot.route().steps().size());
    Assertions.assertEquals(
        List.of(AgentRole.PLANNER, AgentRole.CODER, AgentRole.REVIEWER),
        snapshot.availableNextActions().stream()
            .filter(action -> !action.complete())
            .map(AgentRunAvailableAction::role)
            .distinct()
            .toList());
    Assertions.assertTrue(snapshot.availableNextActions().stream().anyMatch(AgentRunAvailableAction::complete));
    Assertions.assertEquals(snapshot.events(), observedEvents);
    Assertions.assertEquals(
        "intentforge.prompt.manager.in-memory",
        snapshot.contextPack().runtimeSelection().get(RuntimeCapability.PROMPT_MANAGER).orElseThrow().id());
    Assertions.assertEquals(
        "intentforge.tool.registry.in-memory",
        snapshot.contextPack().runtimeSelection().get(RuntimeCapability.TOOL_REGISTRY).orElseThrow().id());
    Assertions.assertEquals(
        Map.of(
            "PROMPT_MANAGER", "intentforge.prompt.manager.in-memory",
            "MODEL_MANAGER", "intentforge.model.manager.in-memory",
            "MODEL_PROVIDER_REGISTRY", "intentforge.model-provider.registry.in-memory",
            "TOOL_REGISTRY", "intentforge.tool.registry.in-memory"),
        snapshot.events().stream()
            .filter(event -> event.type() == AgentRunEventType.CONTEXT_RESOLVED)
            .findFirst()
            .orElseThrow()
            .metadata()
            .get("selectedRuntimeIds"));
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
    AgentRunSnapshot pausedAfterReview = gateway.resume(
        pausedAfterPlanner.runId(),
        new AgentRunTransition("Review the plan before coding", null, AgentRole.REVIEWER, false),
        event -> {
        });

    Assertions.assertEquals(AgentRunStatus.AWAITING_USER, pausedAfterReview.status());
    Assertions.assertEquals(2, pausedAfterReview.nextStepIndex());
    Assertions.assertEquals(
        List.of(AgentRole.PLANNER, AgentRole.REVIEWER),
        pausedAfterReview.route().steps().stream().map(step -> step.role()).toList());
    Assertions.assertTrue(pausedAfterReview.state().messages().stream()
        .map(AgentRunMessage::content)
        .toList()
        .contains("Review the plan before coding"));
    Assertions.assertTrue(pausedAfterReview.events().stream().anyMatch(event ->
        event.type() == AgentRunEventType.RUN_RESUMED
            && AgentRole.REVIEWER.name().equals(event.metadata().get("selectedRole"))));

    AgentRunSnapshot completed = gateway.resume(
        pausedAfterPlanner.runId(),
        new AgentRunTransition("Plan looks good, stop here", null, null, true),
        event -> {
        });

    Assertions.assertEquals(AgentRunStatus.COMPLETED, completed.status());
    Assertions.assertEquals(2, completed.state().messages().size());
    Assertions.assertEquals(AgentRunEventType.RUN_COMPLETED, completed.events().getLast().type());
  }

  @Test
  void shouldAllowUserToSwitchToSpecificAllowedAgent() throws Exception {
    AgentRunGateway gateway = createGateway();

    AgentRunSnapshot pausedAfterPlanner = gateway.start(new AgentTask(
        "task-1",
        "session-1",
        null,
        Files.createTempDirectory("agent-run-switch-agent"),
        TaskMode.FULL,
        "Implement event-driven run model",
        null,
        Map.of()), event -> {
    });

    AgentRunSnapshot pausedAfterAlternateCoder = gateway.resume(
        pausedAfterPlanner.runId(),
        new AgentRunTransition("Use the alternate coder", "intentforge.native.coder.alt", null, false),
        event -> {
        });

    Assertions.assertEquals(AgentRunStatus.AWAITING_USER, pausedAfterAlternateCoder.status());
    Assertions.assertEquals(
        "intentforge.native.coder.alt",
        pausedAfterAlternateCoder.route().steps().getLast().agentId());
    Assertions.assertTrue(pausedAfterAlternateCoder.events().stream().anyMatch(event ->
        event.type() == AgentRunEventType.RUN_RESUMED
            && "intentforge.native.coder.alt".equals(event.metadata().get("selectedAgentId"))));
  }

  @Test
  void shouldRejectSelectingDisallowedAgent() throws Exception {
    AgentRunGateway gateway = createGateway();

    AgentRunSnapshot pausedAfterPlanner = gateway.start(new AgentTask(
        "task-1",
        "session-1",
        null,
        Files.createTempDirectory("agent-run-disallowed-agent"),
        TaskMode.FULL,
        "Implement event-driven run model",
        null,
        Map.of()), event -> {
    });

    AgentExecutionException exception = Assertions.assertThrows(
        AgentExecutionException.class,
        () -> gateway.resume(
            pausedAfterPlanner.runId(),
            new AgentRunTransition("Use a forbidden agent", "intentforge.native.judge", null, false),
            event -> {
            }));

    Assertions.assertTrue(exception.getMessage().contains("allowed"));
  }

  @Test
  void shouldRejectTransitionWithoutSelectionWhenPaused() throws Exception {
    AgentRunGateway gateway = createGateway();

    AgentRunSnapshot pausedAfterPlanner = gateway.start(new AgentTask(
        "task-1",
        "session-1",
        null,
        Files.createTempDirectory("agent-run-missing-selection"),
        TaskMode.FULL,
        "Implement event-driven run model",
        null,
        Map.of()), event -> {
    });

    IllegalArgumentException exception = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> gateway.resume(pausedAfterPlanner.runId(), new AgentRunTransition("continue", null, null, false), event -> {
        }));

    Assertions.assertTrue(exception.getMessage().contains("nextRole"));
  }

  @Test
  void shouldStillSupportLegacyAutoResumeForSynchronousCompatibility() throws Exception {
    AgentRunGateway gateway = createGateway();

    AgentRunSnapshot pausedAfterPlanner = gateway.start(new AgentTask(
        "task-1",
        "session-1",
        null,
        Files.createTempDirectory("agent-run-legacy-auto"),
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

    Assertions.assertEquals(AgentRunStatus.AWAITING_USER, completed.status());
    Assertions.assertEquals(3, completed.state().decisions().size());
    Assertions.assertEquals(2, completed.state().messages().size());
    AgentRunSnapshot finished = gateway.resume(pausedAfterPlanner.runId(), "Ship it", event -> {
    });
    Assertions.assertEquals(AgentRunStatus.COMPLETED, finished.status());
    Assertions.assertEquals(AgentRunEventType.RUN_COMPLETED, finished.events().getLast().type());
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
    AgentRunSnapshot pausedAfterPlanner = gateway.start(new AgentTask(
        "task-1",
        "session-1",
        null,
        Files.createTempDirectory("agent-run-invalid"),
        TaskMode.PLAN_ONLY,
        "Plan only",
        null,
        Map.of()), event -> {
    });
    Assertions.assertEquals(AgentRunStatus.AWAITING_USER, pausedAfterPlanner.status());

    AgentRunSnapshot completed = gateway.resume(
        pausedAfterPlanner.runId(),
        new AgentRunTransition("Plan is enough", null, null, true),
        event -> {
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
        List.of("intentforge.native.planner", "intentforge.native.coder", "intentforge.native.coder.alt", "intentforge.native.reviewer"),
        List.of("prompt-1"),
        List.of("intentforge.fs.list"),
        List.of("model-1"),
        List.of("provider-1"),
        List.of(),
        Map.of("review.level", "mvp"));

    return new DefaultAgentRunGateway(
        sessionManager,
        spaceResolver,
        new StaticRuntimeResolver(
            promptManager,
            modelManager,
            providerRegistry,
            new StubToolGateway(List.of(new ToolDefinition("intentforge.fs.list", "list", Map.of(), false)))),
        new StageRoutingAgentRouter(),
        List.of(
            new StubExecutor("intentforge.native.planner", AgentRole.PLANNER, true),
            new StubExecutor("intentforge.native.coder", AgentRole.CODER, false),
            new StubExecutor("intentforge.native.coder.alt", AgentRole.CODER, false),
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

  private static final class StaticRuntimeResolver implements AgentRuntimeResolver {
    private final InMemoryPromptManager promptManager;
    private final InMemoryModelManager modelManager;
    private final InMemoryModelProviderRegistry providerRegistry;
    private final StubToolGateway toolGateway;

    private StaticRuntimeResolver(
        InMemoryPromptManager promptManager,
        InMemoryModelManager modelManager,
        InMemoryModelProviderRegistry providerRegistry,
        StubToolGateway toolGateway
    ) {
      this.promptManager = promptManager;
      this.modelManager = modelManager;
      this.providerRegistry = providerRegistry;
      this.toolGateway = toolGateway;
    }

    @Override
    public ResolvedAgentRuntime resolve(ResolvedSpaceProfile resolvedSpaceProfile) {
      return new ResolvedAgentRuntime(
          new ResolvedRuntimeSelection(Map.of(
              RuntimeCapability.PROMPT_MANAGER,
              new RuntimeImplementationDescriptor(
                  "intentforge.prompt.manager.in-memory",
                  RuntimeCapability.PROMPT_MANAGER,
                  "Prompt",
                  promptManager.getClass().getName(),
                  Map.of()),
              RuntimeCapability.MODEL_MANAGER,
              new RuntimeImplementationDescriptor(
                  "intentforge.model.manager.in-memory",
                  RuntimeCapability.MODEL_MANAGER,
                  "Model",
                  modelManager.getClass().getName(),
                  Map.of()),
              RuntimeCapability.MODEL_PROVIDER_REGISTRY,
              new RuntimeImplementationDescriptor(
                  "intentforge.model-provider.registry.in-memory",
                  RuntimeCapability.MODEL_PROVIDER_REGISTRY,
                  "Provider",
                  providerRegistry.getClass().getName(),
                  Map.of()),
              RuntimeCapability.TOOL_REGISTRY,
              new RuntimeImplementationDescriptor(
                  "intentforge.tool.registry.in-memory",
                  RuntimeCapability.TOOL_REGISTRY,
                  "Tool",
                  toolGateway.getClass().getName(),
                  Map.of()))),
          promptManager,
          modelManager,
          providerRegistry,
          toolGateway);
    }
  }
}
