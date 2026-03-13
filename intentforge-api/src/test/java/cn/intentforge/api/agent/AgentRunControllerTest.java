package cn.intentforge.api.agent;

import cn.intentforge.agent.core.AgentExecutionException;
import cn.intentforge.agent.core.AgentExecutionState;
import cn.intentforge.agent.core.AgentRole;
import cn.intentforge.agent.core.AgentRoute;
import cn.intentforge.agent.core.AgentRouteStep;
import cn.intentforge.agent.core.AgentRunAvailableAction;
import cn.intentforge.agent.core.AgentRunEvent;
import cn.intentforge.agent.core.AgentRunEventType;
import cn.intentforge.agent.core.AgentRunGateway;
import cn.intentforge.agent.core.AgentRunObserver;
import cn.intentforge.agent.core.AgentRunSnapshot;
import cn.intentforge.agent.core.AgentRunStatus;
import cn.intentforge.agent.core.AgentTask;
import cn.intentforge.agent.core.AgentRunTransition;
import cn.intentforge.agent.core.ContextPack;
import cn.intentforge.config.ResolvedRuntimeSelection;
import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.config.RuntimeImplementationDescriptor;
import cn.intentforge.session.model.Session;
import cn.intentforge.session.model.SessionDraft;
import cn.intentforge.session.model.SessionMessageDraft;
import cn.intentforge.session.model.SessionQuery;
import cn.intentforge.session.model.SessionStatus;
import cn.intentforge.session.registry.SessionManager;
import cn.intentforge.space.ResolvedSpaceProfile;
import cn.intentforge.space.SpaceType;
import cn.intentforge.tool.core.gateway.ToolGateway;
import cn.intentforge.tool.core.model.ToolCallRequest;
import cn.intentforge.tool.core.model.ToolCallResult;
import cn.intentforge.tool.core.model.ToolExecutionContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgentRunControllerTest {
  @Test
  void shouldAutoCreateSessionAndMapRunResponse() throws Exception {
    InMemorySessionManagerStub sessionManager = new InMemorySessionManagerStub();
    AgentRunGatewayStub gateway = new AgentRunGatewayStub(sessionManager);
    AgentRunController controller = new AgentRunController(new AgentRunApplicationService(gateway, sessionManager));
    Path workspace = Files.createTempDirectory("agent-run-controller-workspace");

    AgentRunResponse response = controller.createRun(
        new AgentRunCreateRequest(
            "task-1",
            null,
            "application-alpha",
            workspace.toString(),
            "FULL",
            "Implement event-driven server",
            null,
            Map.of("story", "IF-601")),
        AgentRunObserver.NOOP);

    Assertions.assertEquals("agent-run-1", response.runId());
    Assertions.assertEquals("task-1", response.taskId());
    Assertions.assertNotNull(response.sessionId());
    Assertions.assertEquals(response.sessionId(), gateway.startedTask.sessionId());
    Assertions.assertEquals("application-alpha", gateway.startedTask.spaceId());
    Assertions.assertEquals(
        "application-alpha",
        sessionManager.find(response.sessionId()).orElseThrow().spaceId());
    Assertions.assertEquals("/api/agent-runs/agent-run-1/events", response.eventsPath());
    Assertions.assertEquals("PROMPT_MANAGER", response.selectedRuntimes().getFirst().capability());
    Assertions.assertEquals("intentforge.native.planner", response.selectedRouteSteps().getFirst().agentId());
    Assertions.assertEquals("CODER", response.availableNextActions().stream()
        .filter(action -> "intentforge.native.coder".equals(action.agentId()))
        .findFirst()
        .orElseThrow()
        .role());
    Assertions.assertEquals("RUN_CREATED", response.events().getFirst().type());
  }

  @Test
  void shouldRejectMissingSpaceWhenAutoCreatingSession() throws Exception {
    InMemorySessionManagerStub sessionManager = new InMemorySessionManagerStub();
    AgentRunGatewayStub gateway = new AgentRunGatewayStub(sessionManager);
    AgentRunController controller = new AgentRunController(new AgentRunApplicationService(gateway, sessionManager));
    Path workspace = Files.createTempDirectory("agent-run-controller-missing-space");

    AgentApiException exception = Assertions.assertThrows(
        AgentApiException.class,
        () -> controller.createRun(
            new AgentRunCreateRequest(
                "task-1",
                null,
                null,
                workspace.toString(),
                "FULL",
                "Implement event-driven server",
                null,
                Map.of()),
            AgentRunObserver.NOOP));

    Assertions.assertEquals(400, exception.statusCode());
    Assertions.assertEquals("AGENT_RUN_REQUEST_INVALID", exception.error().code());
    Assertions.assertTrue(exception.error().message().contains("spaceId"));
  }

  @Test
  void shouldRequireExplicitNextSelectionWhenResumingRun() {
    IllegalArgumentException exception = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new AgentRunFeedbackRequest("continue", null, null, false));

    Assertions.assertTrue(exception.getMessage().contains("nextRole"));
  }

  @Test
  void shouldMapSelectedNextActionWhenResumingRun() {
    InMemorySessionManagerStub sessionManager = new InMemorySessionManagerStub();
    AgentRunGatewayStub gateway = new AgentRunGatewayStub(sessionManager);
    AgentRunController controller = new AgentRunController(new AgentRunApplicationService(gateway, sessionManager));

    AgentRunResponse response = controller.resumeRun(
        "agent-run-1",
        new AgentRunFeedbackRequest("switch to review", "REVIEWER", null, false),
        AgentRunObserver.NOOP);

    Assertions.assertEquals("REVIEWER", gateway.resumeTransition.nextRole().name());
    Assertions.assertEquals(
        List.of("intentforge.native.planner", "intentforge.native.reviewer"),
        response.selectedRouteSteps().stream().map(AgentRouteStepResponse::agentId).toList());
  }

  private static final class AgentRunGatewayStub implements AgentRunGateway {
    private final SessionManager sessionManager;
    private AgentTask startedTask;
    private AgentRunTransition resumeTransition;

    private AgentRunGatewayStub(SessionManager sessionManager) {
      this.sessionManager = sessionManager;
    }

    @Override
    public AgentRunSnapshot start(AgentTask task, AgentRunObserver observer) {
      this.startedTask = task;
      Session session = sessionManager.find(task.sessionId()).orElseThrow();
      return snapshot(task, session);
    }

    @Override
    public AgentRunSnapshot get(String runId) {
      throw new AgentExecutionException("run not found: " + runId);
    }

    @Override
    public AgentRunSnapshot resume(String runId, String feedback, AgentRunObserver observer) {
      throw new UnsupportedOperationException("legacy resume not needed for this test");
    }

    @Override
    public AgentRunSnapshot resume(String runId, AgentRunTransition transition, AgentRunObserver observer) {
      this.resumeTransition = transition;
      Session session = sessionManager.create(new SessionDraft("session-1", "Coding", "application-alpha", Map.of()));
      AgentTask task = new AgentTask(
          "task-1",
          session.id(),
          "application-alpha",
          Path.of("/tmp/workspace"),
          cn.intentforge.agent.core.TaskMode.FULL,
          "Implement event-driven server",
          null,
          Map.of());
      return snapshot(task, session, List.of(
          new AgentRouteStep(1, "intentforge.native.planner", AgentRole.PLANNER, "planner step"),
          new AgentRouteStep(2, "intentforge.native.reviewer", AgentRole.REVIEWER, "user selected reviewer")));
    }

    @Override
    public AgentRunSnapshot cancel(String runId, String reason, AgentRunObserver observer) {
      throw new UnsupportedOperationException("cancel not needed for this test");
    }

    private static AgentRunSnapshot snapshot(AgentTask task, Session session) {
      return snapshot(task, session, List.of(
          new AgentRouteStep(1, "intentforge.native.planner", AgentRole.PLANNER, "planner step")));
    }

    private static AgentRunSnapshot snapshot(AgentTask task, Session session, List<AgentRouteStep> routeSteps) {
      Instant now = Instant.parse("2026-03-13T00:00:00Z");
      RuntimeImplementationDescriptor runtime = new RuntimeImplementationDescriptor(
          "intentforge.prompt.manager.in-memory",
          RuntimeCapability.PROMPT_MANAGER,
          "In-Memory Prompt Manager",
          "cn.intentforge.prompt.local.registry.InMemoryPromptManager",
          Map.of("builtin", "true"));
      ContextPack contextPack = new ContextPack(
          task,
          session,
          new ResolvedSpaceProfile(
              session.spaceId(),
              SpaceType.APPLICATION,
              List.of("company-root", session.spaceId()),
              List.of(),
              List.of("intentforge.native.planner"),
              List.of(),
              List.of(),
              List.of(),
              List.of(),
              List.of(),
              Map.of()),
          new ResolvedRuntimeSelection(Map.of(RuntimeCapability.PROMPT_MANAGER, runtime)),
          List.of(),
          List.of(),
          List.of(),
          List.of(),
          new ToolGateway() {
            @Override
            public ToolCallResult execute(ToolCallRequest request) {
              throw new UnsupportedOperationException("tool execution not needed for this test");
            }

            @Override
            public List<cn.intentforge.tool.core.model.ToolDefinition> listTools() {
              return List.of();
            }
          },
          ToolExecutionContext.create(task.workspaceRoot()));
      return new AgentRunSnapshot(
          "agent-run-1",
          task,
          AgentRunStatus.AWAITING_USER,
          contextPack,
          new AgentRoute(
              "stage-routing",
              routeSteps),
          AgentExecutionState.empty(),
          List.of(new AgentRunEvent(
              "agent-run-1",
              1L,
              AgentRunEventType.RUN_CREATED,
              AgentRunStatus.RUNNING,
              "run created",
              Map.of("taskId", task.id()),
              now)),
          "awaiting user feedback before continuing to CODER",
          1,
          List.of(
              new AgentRunAvailableAction("intentforge.native.planner", AgentRole.PLANNER, false, false, "iterate on plan"),
              new AgentRunAvailableAction("intentforge.native.coder", AgentRole.CODER, true, false, "implement the approved plan"),
              new AgentRunAvailableAction("intentforge.native.reviewer", AgentRole.REVIEWER, false, false, "review before implementation"),
              AgentRunAvailableAction.complete(true, "stop after the current checkpoint")),
          now,
          now);
    }
  }

  private static final class InMemorySessionManagerStub implements SessionManager {
    private final Map<String, Session> sessionsById = new LinkedHashMap<>();

    @Override
    public Session create(SessionDraft draft) {
      Instant now = Instant.parse("2026-03-13T00:00:00Z");
      Session session = new Session(
          draft.id(),
          draft.title(),
          draft.spaceId(),
          SessionStatus.ACTIVE,
          List.of(),
          draft.metadata(),
          now,
          now);
      sessionsById.put(session.id(), session);
      return session;
    }

    @Override
    public Optional<Session> find(String id) {
      return Optional.ofNullable(sessionsById.get(id));
    }

    @Override
    public List<Session> list(SessionQuery query) {
      return sessionsById.values().stream().filter(session -> session.matches(query)).toList();
    }

    @Override
    public Session appendMessage(String sessionId, SessionMessageDraft messageDraft) {
      throw new UnsupportedOperationException("appendMessage not needed for this test");
    }

    @Override
    public Session archive(String sessionId) {
      throw new UnsupportedOperationException("archive not needed for this test");
    }

    @Override
    public void delete(String sessionId) {
      sessionsById.remove(sessionId);
    }
  }
}
