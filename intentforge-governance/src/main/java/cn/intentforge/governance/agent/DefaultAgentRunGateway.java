package cn.intentforge.governance.agent;

import cn.intentforge.agent.core.AgentDescriptor;
import cn.intentforge.agent.core.AgentExecutionException;
import cn.intentforge.agent.core.AgentExecutionState;
import cn.intentforge.agent.core.AgentExecutor;
import cn.intentforge.agent.core.AgentRoute;
import cn.intentforge.agent.core.AgentRouteStep;
import cn.intentforge.agent.core.AgentRunEvent;
import cn.intentforge.agent.core.AgentRunEventType;
import cn.intentforge.agent.core.AgentRunGateway;
import cn.intentforge.agent.core.AgentRunMessage;
import cn.intentforge.agent.core.AgentRunMessageRole;
import cn.intentforge.agent.core.AgentRunObserver;
import cn.intentforge.agent.core.AgentRunSnapshot;
import cn.intentforge.agent.core.AgentRunStatus;
import cn.intentforge.agent.core.AgentStepResult;
import cn.intentforge.agent.core.AgentTask;
import cn.intentforge.agent.core.ContextPack;
import cn.intentforge.model.catalog.ModelDescriptor;
import cn.intentforge.model.provider.ModelProvider;
import cn.intentforge.model.provider.ModelProviderDescriptor;
import cn.intentforge.model.provider.registry.ModelProviderRegistry;
import cn.intentforge.model.registry.ModelManager;
import cn.intentforge.prompt.model.PromptDefinition;
import cn.intentforge.prompt.registry.PromptManager;
import cn.intentforge.session.model.Session;
import cn.intentforge.session.registry.SessionManager;
import cn.intentforge.space.ResolvedSpaceProfile;
import cn.intentforge.space.SpaceResolver;
import cn.intentforge.tool.core.gateway.ToolGateway;
import cn.intentforge.tool.core.model.ToolDefinition;
import cn.intentforge.tool.core.model.ToolExecutionContext;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default in-memory event-driven run gateway used to orchestrate multi-turn agent execution.
 */
public final class DefaultAgentRunGateway implements AgentRunGateway {
  private final SessionManager sessionManager;
  private final SpaceResolver spaceResolver;
  private final PromptManager promptManager;
  private final ModelManager modelManager;
  private final ModelProviderRegistry modelProviderRegistry;
  private final ToolGateway toolGateway;
  private final AgentRouter agentRouter;
  private final Map<String, AgentExecutor> executorsById;
  private final List<AgentDescriptor> descriptors;
  private final Clock clock;
  private final AtomicLong runSequence = new AtomicLong();
  private final Map<String, StoredRun> runsById = new LinkedHashMap<>();

  /**
   * Creates the gateway with required runtime collaborators and the system clock.
   *
   * @param sessionManager session manager
   * @param spaceResolver space resolver
   * @param promptManager prompt manager
   * @param modelManager model manager
   * @param modelProviderRegistry model provider registry
   * @param toolGateway tool gateway
   * @param agentRouter route selector
   * @param executors available agent executors
   */
  public DefaultAgentRunGateway(
      SessionManager sessionManager,
      SpaceResolver spaceResolver,
      PromptManager promptManager,
      ModelManager modelManager,
      ModelProviderRegistry modelProviderRegistry,
      ToolGateway toolGateway,
      AgentRouter agentRouter,
      List<AgentExecutor> executors
  ) {
    this(
        sessionManager,
        spaceResolver,
        promptManager,
        modelManager,
        modelProviderRegistry,
        toolGateway,
        agentRouter,
        executors,
        Clock.systemUTC());
  }

  /**
   * Creates the gateway with required runtime collaborators and a custom clock.
   *
   * @param sessionManager session manager
   * @param spaceResolver space resolver
   * @param promptManager prompt manager
   * @param modelManager model manager
   * @param modelProviderRegistry model provider registry
   * @param toolGateway tool gateway
   * @param agentRouter route selector
   * @param executors available agent executors
   * @param clock clock used for event and snapshot timestamps
   */
  public DefaultAgentRunGateway(
      SessionManager sessionManager,
      SpaceResolver spaceResolver,
      PromptManager promptManager,
      ModelManager modelManager,
      ModelProviderRegistry modelProviderRegistry,
      ToolGateway toolGateway,
      AgentRouter agentRouter,
      List<AgentExecutor> executors,
      Clock clock
  ) {
    this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager must not be null");
    this.spaceResolver = Objects.requireNonNull(spaceResolver, "spaceResolver must not be null");
    this.promptManager = Objects.requireNonNull(promptManager, "promptManager must not be null");
    this.modelManager = Objects.requireNonNull(modelManager, "modelManager must not be null");
    this.modelProviderRegistry = Objects.requireNonNull(modelProviderRegistry, "modelProviderRegistry must not be null");
    this.toolGateway = Objects.requireNonNull(toolGateway, "toolGateway must not be null");
    this.agentRouter = Objects.requireNonNull(agentRouter, "agentRouter must not be null");
    this.executorsById = indexExecutors(executors);
    this.descriptors = this.executorsById.values().stream().map(AgentExecutor::descriptor).toList();
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  /**
   * Starts one run and forwards emitted events to the observer.
   *
   * @param task task request
   * @param observer run observer
   * @return latest run snapshot
   */
  @Override
  public synchronized AgentRunSnapshot start(AgentTask task, AgentRunObserver observer) {
    AgentTask nonNullTask = Objects.requireNonNull(task, "task must not be null");
    AgentRunObserver nonNullObserver = observer == null ? AgentRunObserver.NOOP : observer;
    Session session = sessionManager.find(nonNullTask.sessionId())
        .orElseThrow(() -> new AgentExecutionException("session not found: " + nonNullTask.sessionId()));
    String effectiveSpaceId = nonNullTask.spaceId() == null ? session.spaceId() : nonNullTask.spaceId();
    if (effectiveSpaceId == null) {
      throw new AgentExecutionException("spaceId must be available from task or session");
    }

    AgentTask effectiveTask = nonNullTask.spaceId() == null
        ? new AgentTask(
            nonNullTask.id(),
            nonNullTask.sessionId(),
            effectiveSpaceId,
            nonNullTask.workspaceRoot(),
            nonNullTask.mode(),
            nonNullTask.intent(),
            nonNullTask.targetAgentId(),
            nonNullTask.metadata())
        : nonNullTask;

    ResolvedSpaceProfile resolvedSpaceProfile = spaceResolver.resolve(effectiveSpaceId);
    ContextPack contextPack = new ContextPack(
        effectiveTask,
        session,
        resolvedSpaceProfile,
        resolvePrompts(resolvedSpaceProfile),
        resolveModels(resolvedSpaceProfile),
        resolveProviders(resolvedSpaceProfile),
        resolveTools(resolvedSpaceProfile),
        ToolExecutionContext.create(effectiveTask.workspaceRoot()));
    AgentRoute route = agentRouter.route(effectiveTask, contextPack, descriptors);

    String runId = "agent-run-" + runSequence.incrementAndGet();
    Instant now = Instant.now(clock);
    StoredRun run = new StoredRun(
        runId,
        effectiveTask,
        contextPack,
        route,
        AgentExecutionState.empty(),
        AgentRunStatus.RUNNING,
        null,
        0,
        now,
        now);
    runsById.put(runId, run);

    emit(run, nonNullObserver, AgentRunEventType.RUN_CREATED, AgentRunStatus.RUNNING, "run created", Map.of(
        "taskId", effectiveTask.id(),
        "mode", effectiveTask.mode().name()));
    emit(run, nonNullObserver, AgentRunEventType.CONTEXT_RESOLVED, AgentRunStatus.RUNNING, "context resolved", Map.of(
        "spaceId", effectiveTask.spaceId(),
        "promptCount", String.valueOf(contextPack.prompts().size()),
        "modelCount", String.valueOf(contextPack.models().size()),
        "toolCount", String.valueOf(contextPack.tools().size())));
    emit(run, nonNullObserver, AgentRunEventType.ROUTE_SELECTED, AgentRunStatus.RUNNING, "route selected", Map.of(
        "strategy", route.strategy(),
        "steps", String.valueOf(route.steps().size())));
    return executeUntilCheckpoint(run, nonNullObserver);
  }

  /**
   * Loads one run snapshot by identifier.
   *
   * @param runId run identifier
   * @return latest run snapshot
   */
  @Override
  public synchronized AgentRunSnapshot get(String runId) {
    return requireRun(runId).snapshot();
  }

  /**
   * Resumes one paused run and forwards emitted events to the observer.
   *
   * @param runId run identifier
   * @param feedback optional user feedback
   * @param observer run observer
   * @return latest run snapshot
   */
  @Override
  public synchronized AgentRunSnapshot resume(String runId, String feedback, AgentRunObserver observer) {
    StoredRun run = requireRun(runId);
    AgentRunObserver nonNullObserver = observer == null ? AgentRunObserver.NOOP : observer;
    if (run.status != AgentRunStatus.AWAITING_USER) {
      throw new AgentExecutionException("run is not awaiting user input: " + runId);
    }
    String normalizedFeedback = normalize(feedback);
    if (normalizedFeedback != null) {
      AgentRunMessage message = new AgentRunMessage(
          run.runId + "-message-" + (run.state.messages().size() + 1),
          AgentRunMessageRole.USER,
          normalizedFeedback,
          Instant.now(clock),
          Map.of("turn", String.valueOf(run.state.messages().size() + 1)));
      run.state = run.state.appendMessage(message);
      emit(run, nonNullObserver, AgentRunEventType.USER_FEEDBACK_RECEIVED, AgentRunStatus.RUNNING, "user feedback received", Map.of(
          "messageId", message.id()));
    }
    run.status = AgentRunStatus.RUNNING;
    run.awaitingReason = null;
    emit(run, nonNullObserver, AgentRunEventType.RUN_RESUMED, AgentRunStatus.RUNNING, "run resumed", Map.of(
        "nextStepIndex", String.valueOf(run.nextStepIndex)));
    return executeUntilCheckpoint(run, nonNullObserver);
  }

  /**
   * Cancels one run and forwards emitted events to the observer.
   *
   * @param runId run identifier
   * @param reason cancellation reason
   * @param observer run observer
   * @return latest run snapshot
   */
  @Override
  public synchronized AgentRunSnapshot cancel(String runId, String reason, AgentRunObserver observer) {
    StoredRun run = requireRun(runId);
    AgentRunObserver nonNullObserver = observer == null ? AgentRunObserver.NOOP : observer;
    if (run.status.isTerminal()) {
      throw new AgentExecutionException("run is already terminal: " + runId);
    }
    String normalizedReason = normalize(reason);
    run.status = AgentRunStatus.CANCELLED;
    run.awaitingReason = normalizedReason == null ? "cancelled" : normalizedReason;
    emit(run, nonNullObserver, AgentRunEventType.RUN_CANCELLED, AgentRunStatus.CANCELLED, "run cancelled", Map.of(
        "reason", run.awaitingReason));
    return run.snapshot();
  }

  private AgentRunSnapshot executeUntilCheckpoint(StoredRun run, AgentRunObserver observer) {
    while (run.nextStepIndex < run.route.steps().size()) {
      AgentRouteStep step = run.route.steps().get(run.nextStepIndex);
      emit(run, observer, AgentRunEventType.STAGE_STARTED, AgentRunStatus.RUNNING, "stage started", Map.of(
          "agentId", step.agentId(),
          "role", step.role().name(),
          "order", String.valueOf(step.order())));
      AgentExecutor executor = executorsById.get(step.agentId());
      if (executor == null) {
        run.status = AgentRunStatus.FAILED;
        run.awaitingReason = "route references unknown agent: " + step.agentId();
        emit(run, observer, AgentRunEventType.RUN_FAILED, AgentRunStatus.FAILED, "run failed", Map.of(
            "reason", run.awaitingReason));
        return run.snapshot();
      }

      try {
        AgentStepResult stepResult = executor.execute(run.contextPack, run.state);
        run.state = run.state.merge(stepResult);
      } catch (Exception ex) {
        run.status = AgentRunStatus.FAILED;
        run.awaitingReason = normalize(ex.getMessage());
        emit(run, observer, AgentRunEventType.RUN_FAILED, AgentRunStatus.FAILED, "run failed", Map.of(
            "reason", run.awaitingReason == null ? ex.getClass().getSimpleName() : run.awaitingReason));
        return run.snapshot();
      }

      emit(run, observer, AgentRunEventType.STAGE_COMPLETED, AgentRunStatus.RUNNING, "stage completed", Map.of(
          "agentId", step.agentId(),
          "role", step.role().name(),
          "decisionCount", String.valueOf(run.state.decisions().size()),
          "artifactCount", String.valueOf(run.state.artifacts().size()),
          "toolCallCount", String.valueOf(run.state.toolCalls().size())));

      run.nextStepIndex++;
      if (run.nextStepIndex < run.route.steps().size()) {
        AgentRouteStep nextStep = run.route.steps().get(run.nextStepIndex);
        run.status = AgentRunStatus.AWAITING_USER;
        run.awaitingReason = "awaiting user feedback before continuing to " + nextStep.role();
        emit(run, observer, AgentRunEventType.AWAITING_USER, AgentRunStatus.AWAITING_USER, run.awaitingReason, Map.of(
            "nextAgentId", nextStep.agentId(),
            "nextRole", nextStep.role().name()));
        return run.snapshot();
      }
    }

    run.status = AgentRunStatus.COMPLETED;
    run.awaitingReason = null;
    emit(run, observer, AgentRunEventType.RUN_COMPLETED, AgentRunStatus.COMPLETED, "run completed", Map.of(
        "decisionCount", String.valueOf(run.state.decisions().size()),
        "artifactCount", String.valueOf(run.state.artifacts().size())));
    return run.snapshot();
  }

  private void emit(
      StoredRun run,
      AgentRunObserver observer,
      AgentRunEventType type,
      AgentRunStatus status,
      String message,
      Map<String, Object> metadata
  ) {
    run.updatedAt = Instant.now(clock);
    AgentRunEvent event = new AgentRunEvent(
        run.runId,
        run.events.size() + 1L,
        type,
        status,
        message,
        metadata,
        run.updatedAt);
    run.events.add(event);
    observer.onEvent(event);
  }

  private StoredRun requireRun(String runId) {
    String normalizedRunId = Objects.requireNonNull(runId, "runId must not be null").trim();
    StoredRun run = runsById.get(normalizedRunId);
    if (run == null) {
      throw new AgentExecutionException("run not found: " + runId);
    }
    return run;
  }

  private List<PromptDefinition> resolvePrompts(ResolvedSpaceProfile resolvedSpaceProfile) {
    if (resolvedSpaceProfile.promptIds().isEmpty()) {
      return promptManager.list(null);
    }
    List<PromptDefinition> resolved = new ArrayList<>();
    for (String promptId : resolvedSpaceProfile.promptIds()) {
      promptManager.findLatest(promptId).ifPresent(resolved::add);
    }
    return List.copyOf(resolved);
  }

  private List<ModelProviderDescriptor> resolveProviders(ResolvedSpaceProfile resolvedSpaceProfile) {
    if (resolvedSpaceProfile.modelProviderIds().isEmpty()) {
      return modelProviderRegistry.list();
    }
    List<ModelProviderDescriptor> resolved = new ArrayList<>();
    for (String providerId : resolvedSpaceProfile.modelProviderIds()) {
      ModelProvider provider = modelProviderRegistry.find(providerId).orElse(null);
      if (provider != null) {
        resolved.add(provider.descriptor());
      }
    }
    return List.copyOf(resolved);
  }

  private List<ModelDescriptor> resolveModels(ResolvedSpaceProfile resolvedSpaceProfile) {
    Set<String> allowedProviderIds = new LinkedHashSet<>(resolvedSpaceProfile.modelProviderIds());
    if (resolvedSpaceProfile.modelIds().isEmpty()) {
      return filterModels(modelManager.list(null), allowedProviderIds);
    }
    List<ModelDescriptor> resolved = new ArrayList<>();
    for (String modelId : resolvedSpaceProfile.modelIds()) {
      ModelDescriptor descriptor = modelManager.find(modelId).orElse(null);
      if (descriptor == null) {
        continue;
      }
      if (!allowedProviderIds.isEmpty() && descriptor.providerId() != null && !allowedProviderIds.contains(descriptor.providerId())) {
        continue;
      }
      resolved.add(descriptor);
    }
    return List.copyOf(resolved);
  }

  private List<ModelDescriptor> filterModels(List<ModelDescriptor> candidates, Set<String> allowedProviderIds) {
    if (allowedProviderIds.isEmpty()) {
      return List.copyOf(candidates);
    }
    List<ModelDescriptor> filtered = new ArrayList<>();
    for (ModelDescriptor descriptor : candidates) {
      if (descriptor.providerId() == null || allowedProviderIds.contains(descriptor.providerId())) {
        filtered.add(descriptor);
      }
    }
    return List.copyOf(filtered);
  }

  private List<ToolDefinition> resolveTools(ResolvedSpaceProfile resolvedSpaceProfile) {
    List<ToolDefinition> tools = toolGateway.listTools();
    if (resolvedSpaceProfile.toolIds().isEmpty()) {
      return tools;
    }
    Map<String, ToolDefinition> toolsById = new LinkedHashMap<>();
    for (ToolDefinition tool : tools) {
      toolsById.put(tool.id(), tool);
    }
    List<ToolDefinition> resolved = new ArrayList<>();
    for (String toolId : resolvedSpaceProfile.toolIds()) {
      ToolDefinition toolDefinition = toolsById.get(toolId);
      if (toolDefinition != null) {
        resolved.add(toolDefinition);
      }
    }
    return List.copyOf(resolved);
  }

  private static Map<String, AgentExecutor> indexExecutors(List<AgentExecutor> executors) {
    List<AgentExecutor> nonNullExecutors = List.copyOf(Objects.requireNonNull(executors, "executors must not be null"));
    Map<String, AgentExecutor> indexed = new LinkedHashMap<>();
    for (AgentExecutor executor : nonNullExecutors) {
      AgentExecutor nonNullExecutor = Objects.requireNonNull(executor, "executor must not be null");
      String agentId = nonNullExecutor.descriptor().id();
      AgentExecutor previous = indexed.putIfAbsent(agentId, nonNullExecutor);
      if (previous != null) {
        throw new IllegalArgumentException("duplicate executor id: " + agentId);
      }
    }
    return Collections.unmodifiableMap(new LinkedHashMap<>(indexed));
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private static final class StoredRun {
    private final String runId;
    private final AgentTask task;
    private final ContextPack contextPack;
    private final AgentRoute route;
    private final List<AgentRunEvent> events = new ArrayList<>();
    private AgentExecutionState state;
    private AgentRunStatus status;
    private String awaitingReason;
    private int nextStepIndex;
    private final Instant createdAt;
    private Instant updatedAt;

    private StoredRun(
        String runId,
        AgentTask task,
        ContextPack contextPack,
        AgentRoute route,
        AgentExecutionState state,
        AgentRunStatus status,
        String awaitingReason,
        int nextStepIndex,
        Instant createdAt,
        Instant updatedAt
    ) {
      this.runId = runId;
      this.task = task;
      this.contextPack = contextPack;
      this.route = route;
      this.state = state;
      this.status = status;
      this.awaitingReason = awaitingReason;
      this.nextStepIndex = nextStepIndex;
      this.createdAt = createdAt;
      this.updatedAt = updatedAt;
    }

    private AgentRunSnapshot snapshot() {
      return new AgentRunSnapshot(
          runId,
          task,
          status,
          contextPack,
          route,
          state,
          List.copyOf(events),
          awaitingReason,
          nextStepIndex,
          createdAt,
          updatedAt);
    }
  }
}
