package cn.intentforge.governance.agent;

import cn.intentforge.agent.core.AgentDescriptor;
import cn.intentforge.agent.core.AgentExecutionException;
import cn.intentforge.agent.core.AgentExecutionState;
import cn.intentforge.agent.core.AgentExecutor;
import cn.intentforge.agent.core.AgentRole;
import cn.intentforge.agent.core.AgentRoute;
import cn.intentforge.agent.core.AgentRouteStep;
import cn.intentforge.agent.core.AgentRunAvailableAction;
import cn.intentforge.agent.core.AgentRunEvent;
import cn.intentforge.agent.core.AgentRunEventType;
import cn.intentforge.agent.core.AgentRunGateway;
import cn.intentforge.agent.core.AgentRunMessage;
import cn.intentforge.agent.core.AgentRunMessageRole;
import cn.intentforge.agent.core.AgentRunObserver;
import cn.intentforge.agent.core.AgentRunSnapshot;
import cn.intentforge.agent.core.AgentRunStatus;
import cn.intentforge.agent.core.AgentRunTransition;
import cn.intentforge.agent.core.AgentStepResult;
import cn.intentforge.agent.core.AgentTask;
import cn.intentforge.agent.core.ContextPack;
import cn.intentforge.agent.core.TaskMode;
import cn.intentforge.config.ResolvedRuntimeSelection;
import cn.intentforge.model.catalog.ModelDescriptor;
import cn.intentforge.model.provider.ModelProvider;
import cn.intentforge.model.provider.ModelProviderDescriptor;
import cn.intentforge.prompt.model.PromptDefinition;
import cn.intentforge.session.model.Session;
import cn.intentforge.session.registry.SessionManager;
import cn.intentforge.space.ResolvedSpaceProfile;
import cn.intentforge.space.SpaceResolver;
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
  private final AgentRuntimeResolver runtimeResolver;
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
   * @param runtimeResolver runtime resolver that selects prompt/model/provider/tool components for each run
   * @param agentRouter route selector
   * @param executors available agent executors
   */
  public DefaultAgentRunGateway(
      SessionManager sessionManager,
      SpaceResolver spaceResolver,
      AgentRuntimeResolver runtimeResolver,
      AgentRouter agentRouter,
      List<AgentExecutor> executors
  ) {
    this(
        sessionManager,
        spaceResolver,
        runtimeResolver,
        agentRouter,
        executors,
        Clock.systemUTC());
  }

  /**
   * Creates the gateway with required runtime collaborators and a custom clock.
   *
   * @param sessionManager session manager
   * @param spaceResolver space resolver
   * @param runtimeResolver runtime resolver that selects prompt/model/provider/tool components for each run
   * @param agentRouter route selector
   * @param executors available agent executors
   * @param clock clock used for event and snapshot timestamps
   */
  public DefaultAgentRunGateway(
      SessionManager sessionManager,
      SpaceResolver spaceResolver,
      AgentRuntimeResolver runtimeResolver,
      AgentRouter agentRouter,
      List<AgentExecutor> executors,
      Clock clock
  ) {
    this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager must not be null");
    this.spaceResolver = Objects.requireNonNull(spaceResolver, "spaceResolver must not be null");
    this.runtimeResolver = Objects.requireNonNull(runtimeResolver, "runtimeResolver must not be null");
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
    ResolvedAgentRuntime resolvedRuntime = runtimeResolver.resolve(resolvedSpaceProfile);
    ContextPack contextPack = new ContextPack(
        effectiveTask,
        session,
        resolvedSpaceProfile,
        resolvedRuntime.runtimeSelection(),
        resolvePrompts(resolvedRuntime, resolvedSpaceProfile),
        resolveModels(resolvedRuntime, resolvedSpaceProfile),
        resolveProviders(resolvedRuntime, resolvedSpaceProfile),
        resolveTools(resolvedRuntime, resolvedSpaceProfile),
        resolvedRuntime.toolGateway(),
        ToolExecutionContext.create(effectiveTask.workspaceRoot()));
    AgentRoute initialRoute = agentRouter.route(effectiveTask, contextPack, descriptors);
    AgentRouteStep firstStep = initialRoute.steps().getFirst();

    String runId = "agent-run-" + runSequence.incrementAndGet();
    Instant now = Instant.now(clock);
    StoredRun run = new StoredRun(
        runId,
        effectiveTask,
        contextPack,
        initialRoute.strategy(),
        List.of(firstStep),
        AgentExecutionState.empty(),
        AgentRunStatus.RUNNING,
        null,
        0,
        List.of(),
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
        "toolCount", String.valueOf(contextPack.tools().size()),
        "selectedRuntimeIds", contextPack.runtimeSelection().selectedImplementationIds()));
    emit(run, nonNullObserver, AgentRunEventType.ROUTE_SELECTED, AgentRunStatus.RUNNING, "route selected", Map.of(
        "strategy", initialRoute.strategy(),
        "steps", String.valueOf(run.routeSteps.size()),
        "selectedAgentId", firstStep.agentId(),
        "selectedRole", firstStep.role().name()));
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
   * Resumes one paused run with an explicit user-selected transition.
   *
   * @param runId run identifier
   * @param transition explicit transition selection
   * @param observer run observer
   * @return latest run snapshot
   */
  @Override
  public synchronized AgentRunSnapshot resume(String runId, AgentRunTransition transition, AgentRunObserver observer) {
    StoredRun run = requireRun(runId);
    AgentRunObserver nonNullObserver = observer == null ? AgentRunObserver.NOOP : observer;
    AgentRunTransition nonNullTransition = Objects.requireNonNull(transition, "transition must not be null");
    requireAwaiting(runId, run);
    AgentRunAvailableAction action = nonNullTransition.complete() ? null : selectAction(run, nonNullTransition);

    if (nonNullTransition.complete()) {
      appendFeedback(run, nonNullTransition.feedback(), nonNullObserver);
      run.availableNextActions = List.of();
      run.status = AgentRunStatus.RUNNING;
      run.awaitingReason = null;
      emit(run, nonNullObserver, AgentRunEventType.RUN_RESUMED, AgentRunStatus.RUNNING, "run resumed", Map.of(
          "nextStepIndex", String.valueOf(run.nextStepIndex),
          "selectedComplete", "true"));
      return completeRun(run, nonNullObserver);
    }

    appendFeedback(run, nonNullTransition.feedback(), nonNullObserver);
    run.availableNextActions = List.of();
    AgentRouteStep selectedStep = new AgentRouteStep(
        run.routeSteps.size() + 1,
        action.agentId(),
        action.role(),
        transitionReason(nonNullTransition, action));
    run.routeSteps.add(selectedStep);
    run.status = AgentRunStatus.RUNNING;
    run.awaitingReason = null;
    emit(run, nonNullObserver, AgentRunEventType.RUN_RESUMED, AgentRunStatus.RUNNING, "run resumed", Map.of(
        "nextStepIndex", String.valueOf(run.nextStepIndex),
        "selectedAgentId", selectedStep.agentId(),
        "selectedRole", selectedStep.role().name(),
        "selectedComplete", "false"));
    return executeUntilCheckpoint(run, nonNullObserver);
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
    requireAwaiting(runId, run);
    AgentRunAvailableAction preferred = selectLegacyPreferredAction(run);
    AgentRunTransition transition = preferred.complete()
        ? new AgentRunTransition(feedback, null, null, true)
        : new AgentRunTransition(feedback, preferred.agentId(), preferred.role(), false);
    return resume(runId, transition, observer);
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
    while (run.nextStepIndex < run.routeSteps.size()) {
      AgentRouteStep step = run.routeSteps.get(run.nextStepIndex);
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
      if (run.nextStepIndex == run.routeSteps.size()) {
        run.availableNextActions = determineAvailableActions(run);
        run.status = AgentRunStatus.AWAITING_USER;
        run.awaitingReason = "awaiting user selection before continuing";
        emit(run, observer, AgentRunEventType.AWAITING_USER, AgentRunStatus.AWAITING_USER, run.awaitingReason, Map.of(
            "availableNextActions", actionMetadata(run.availableNextActions)));
        return run.snapshot();
      }
    }
    return completeRun(run, observer);
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

  private void requireAwaiting(String runId, StoredRun run) {
    if (run.status != AgentRunStatus.AWAITING_USER) {
      throw new AgentExecutionException("run is not awaiting user input: " + runId);
    }
  }

  private void appendFeedback(StoredRun run, String feedback, AgentRunObserver observer) {
    String normalizedFeedback = normalize(feedback);
    if (normalizedFeedback == null) {
      return;
    }
    AgentRunMessage message = new AgentRunMessage(
        run.runId + "-message-" + (run.state.messages().size() + 1),
        AgentRunMessageRole.USER,
        normalizedFeedback,
        Instant.now(clock),
        Map.of("turn", String.valueOf(run.state.messages().size() + 1)));
    run.state = run.state.appendMessage(message);
    emit(run, observer, AgentRunEventType.USER_FEEDBACK_RECEIVED, AgentRunStatus.RUNNING, "user feedback received", Map.of(
        "messageId", message.id()));
  }

  private AgentRunSnapshot completeRun(StoredRun run, AgentRunObserver observer) {
    run.status = AgentRunStatus.COMPLETED;
    run.awaitingReason = null;
    run.availableNextActions = List.of();
    emit(run, observer, AgentRunEventType.RUN_COMPLETED, AgentRunStatus.COMPLETED, "run completed", Map.of(
        "decisionCount", String.valueOf(run.state.decisions().size()),
        "artifactCount", String.valueOf(run.state.artifacts().size())));
    return run.snapshot();
  }

  private AgentRunAvailableAction selectAction(StoredRun run, AgentRunTransition transition) {
    List<AgentRunAvailableAction> available = run.availableNextActions;
    if (available.isEmpty()) {
      throw new AgentExecutionException("run has no available next actions: " + run.runId);
    }
    AgentRunAvailableAction action = transition.nextAgentId() != null
        ? selectByAgentId(available, transition.nextAgentId())
        : selectByRole(available, Objects.requireNonNull(transition.nextRole(), "nextRole must not be null"));
    if (transition.nextRole() != null && !transition.nextRole().equals(action.role())) {
      throw new AgentExecutionException("selected agent does not match requested role: " + transition.nextAgentId());
    }
    return action;
  }

  private AgentRunAvailableAction selectByAgentId(List<AgentRunAvailableAction> actions, String agentId) {
    for (AgentRunAvailableAction action : actions) {
      if (!action.complete() && action.agentId().equals(agentId)) {
        return action;
      }
    }
    throw new AgentExecutionException("selected agent is not allowed by the resolved space: " + agentId);
  }

  private AgentRunAvailableAction selectByRole(List<AgentRunAvailableAction> actions, AgentRole role) {
    AgentRunAvailableAction preferred = null;
    AgentRunAvailableAction firstMatch = null;
    for (AgentRunAvailableAction action : actions) {
      if (action.complete() || action.role() != role) {
        continue;
      }
      if (firstMatch == null) {
        firstMatch = action;
      }
      if (action.preferred()) {
        preferred = action;
        break;
      }
    }
    if (preferred != null) {
      return preferred;
    }
    if (firstMatch != null) {
      return firstMatch;
    }
    throw new AgentExecutionException("selected role is not available at the current checkpoint: " + role);
  }

  private AgentRunAvailableAction selectLegacyPreferredAction(StoredRun run) {
    AgentRunAvailableAction preferred = run.availableNextActions.stream().filter(AgentRunAvailableAction::preferred).findFirst().orElse(null);
    if (preferred != null) {
      return preferred;
    }
    AgentRole lastRole = run.routeSteps.getLast().role();
    AgentRole preferredRole = switch (run.task.mode()) {
      case PLAN_ONLY -> null;
      case IMPLEMENT_ONLY -> lastRole == AgentRole.PLANNER ? AgentRole.CODER : null;
      case REVIEW_ONLY -> null;
      case FULL -> switch (lastRole) {
        case PLANNER -> AgentRole.CODER;
        case CODER -> AgentRole.REVIEWER;
        case REVIEWER, JUDGE -> null;
      };
    };
    if (preferredRole == null) {
      return run.availableNextActions.stream()
          .filter(AgentRunAvailableAction::complete)
          .findFirst()
          .orElseThrow(() -> new AgentExecutionException("run has no completion action: " + run.runId));
    }
    for (AgentRunAvailableAction action : run.availableNextActions) {
      if (!action.complete() && action.role() == preferredRole) {
        return action;
      }
    }
    throw new AgentExecutionException("run has no available action for role: " + preferredRole);
  }

  private List<AgentRunAvailableAction> determineAvailableActions(StoredRun run) {
    List<AgentDescriptor> candidates = allowedDescriptors(run.contextPack.resolvedSpaceProfile());
    AgentRole preferredRole = preferredRoleFor(run.task.mode(), run.routeSteps.getLast().role());
    List<AgentRunAvailableAction> actions = new ArrayList<>(candidates.size() + 1);
    boolean preferredAssigned = false;
    for (AgentDescriptor descriptor : candidates) {
      boolean preferred = !preferredAssigned && preferredRole != null && descriptor.role() == preferredRole;
      actions.add(new AgentRunAvailableAction(
          descriptor.id(),
          descriptor.role(),
          preferred,
          false,
          preferred
              ? "preferred next action for task mode " + run.task.mode()
              : "available by resolved space binding"));
      preferredAssigned = preferredAssigned || preferred;
    }
    actions.add(AgentRunAvailableAction.complete(preferredRole == null, "finish the run at the current checkpoint"));
    return List.copyOf(actions);
  }

  private List<AgentDescriptor> allowedDescriptors(ResolvedSpaceProfile resolvedSpaceProfile) {
    Set<String> allowedAgentIds = new LinkedHashSet<>(resolvedSpaceProfile.agentIds());
    if (allowedAgentIds.isEmpty()) {
      return descriptors;
    }
    List<AgentDescriptor> allowed = new ArrayList<>();
    for (AgentDescriptor descriptor : descriptors) {
      if (allowedAgentIds.contains(descriptor.id())) {
        allowed.add(descriptor);
      }
    }
    return List.copyOf(allowed);
  }

  private AgentRole preferredRoleFor(TaskMode mode, AgentRole lastRole) {
    return switch (mode) {
      case PLAN_ONLY -> null;
      case IMPLEMENT_ONLY -> lastRole == AgentRole.PLANNER ? AgentRole.CODER : null;
      case REVIEW_ONLY -> null;
      case FULL -> switch (lastRole) {
        case PLANNER -> AgentRole.CODER;
        case CODER -> AgentRole.REVIEWER;
        case REVIEWER, JUDGE -> null;
      };
    };
  }

  private String transitionReason(AgentRunTransition transition, AgentRunAvailableAction action) {
    if (transition.nextAgentId() != null) {
      return "user selected agent " + action.agentId();
    }
    return "user selected role " + action.role();
  }

  private List<Map<String, Object>> actionMetadata(List<AgentRunAvailableAction> actions) {
    List<Map<String, Object>> metadata = new ArrayList<>(actions.size());
    for (AgentRunAvailableAction action : actions) {
      metadata.add(Map.of(
          "agentId", action.agentId() == null ? "COMPLETE" : action.agentId(),
          "role", action.role() == null ? "COMPLETE" : action.role().name(),
          "preferred", String.valueOf(action.preferred()),
          "complete", String.valueOf(action.complete()),
          "reason", action.reason()));
    }
    return List.copyOf(metadata);
  }

  private List<PromptDefinition> resolvePrompts(ResolvedAgentRuntime resolvedRuntime, ResolvedSpaceProfile resolvedSpaceProfile) {
    if (resolvedSpaceProfile.promptIds().isEmpty()) {
      return resolvedRuntime.promptManager().list(null);
    }
    List<PromptDefinition> resolved = new ArrayList<>();
    for (String promptId : resolvedSpaceProfile.promptIds()) {
      resolvedRuntime.promptManager().findLatest(promptId).ifPresent(resolved::add);
    }
    return List.copyOf(resolved);
  }

  private List<ModelProviderDescriptor> resolveProviders(ResolvedAgentRuntime resolvedRuntime, ResolvedSpaceProfile resolvedSpaceProfile) {
    if (resolvedSpaceProfile.modelProviderIds().isEmpty()) {
      return resolvedRuntime.modelProviderRegistry().list();
    }
    List<ModelProviderDescriptor> resolved = new ArrayList<>();
    for (String providerId : resolvedSpaceProfile.modelProviderIds()) {
      ModelProvider provider = resolvedRuntime.modelProviderRegistry().find(providerId).orElse(null);
      if (provider != null) {
        resolved.add(provider.descriptor());
      }
    }
    return List.copyOf(resolved);
  }

  private List<ModelDescriptor> resolveModels(ResolvedAgentRuntime resolvedRuntime, ResolvedSpaceProfile resolvedSpaceProfile) {
    Set<String> allowedProviderIds = new LinkedHashSet<>(resolvedSpaceProfile.modelProviderIds());
    if (resolvedSpaceProfile.modelIds().isEmpty()) {
      return filterModels(resolvedRuntime.modelManager().list(null), allowedProviderIds);
    }
    List<ModelDescriptor> resolved = new ArrayList<>();
    for (String modelId : resolvedSpaceProfile.modelIds()) {
      ModelDescriptor descriptor = resolvedRuntime.modelManager().find(modelId).orElse(null);
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

  private List<ToolDefinition> resolveTools(ResolvedAgentRuntime resolvedRuntime, ResolvedSpaceProfile resolvedSpaceProfile) {
    List<ToolDefinition> tools = resolvedRuntime.toolGateway().listTools();
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
    private final String routeStrategy;
    private final List<AgentRouteStep> routeSteps = new ArrayList<>();
    private final List<AgentRunEvent> events = new ArrayList<>();
    private AgentExecutionState state;
    private AgentRunStatus status;
    private String awaitingReason;
    private int nextStepIndex;
    private List<AgentRunAvailableAction> availableNextActions;
    private final Instant createdAt;
    private Instant updatedAt;

    private StoredRun(
        String runId,
        AgentTask task,
        ContextPack contextPack,
        String routeStrategy,
        List<AgentRouteStep> routeSteps,
        AgentExecutionState state,
        AgentRunStatus status,
        String awaitingReason,
        int nextStepIndex,
        List<AgentRunAvailableAction> availableNextActions,
        Instant createdAt,
        Instant updatedAt
    ) {
      this.runId = runId;
      this.task = task;
      this.contextPack = contextPack;
      this.routeStrategy = routeStrategy;
      this.routeSteps.addAll(routeSteps);
      this.state = state;
      this.status = status;
      this.awaitingReason = awaitingReason;
      this.nextStepIndex = nextStepIndex;
      this.availableNextActions = List.copyOf(availableNextActions);
      this.createdAt = createdAt;
      this.updatedAt = updatedAt;
    }

    private AgentRunSnapshot snapshot() {
      return new AgentRunSnapshot(
          runId,
          task,
          status,
          contextPack,
          new AgentRoute(routeStrategy, routeSteps),
          state,
          List.copyOf(events),
          awaitingReason,
          nextStepIndex,
          availableNextActions,
          createdAt,
          updatedAt);
    }
  }
}
