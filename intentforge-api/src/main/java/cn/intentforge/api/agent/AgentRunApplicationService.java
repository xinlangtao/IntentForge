package cn.intentforge.api.agent;

import cn.intentforge.agent.core.AgentExecutionException;
import cn.intentforge.agent.core.AgentRunGateway;
import cn.intentforge.agent.core.AgentRunObserver;
import cn.intentforge.agent.core.AgentRunSnapshot;
import cn.intentforge.agent.core.AgentTask;
import cn.intentforge.agent.core.AgentRunTransition;
import cn.intentforge.agent.core.AgentRole;
import cn.intentforge.agent.core.TaskMode;
import cn.intentforge.api.util.ApiModelSupport;
import cn.intentforge.session.model.Session;
import cn.intentforge.session.model.SessionDraft;
import cn.intentforge.session.registry.SessionManager;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Application service that executes agent run use cases behind the external API.
 */
public final class AgentRunApplicationService {
  private final AgentRunGateway agentRunGateway;
  private final SessionManager sessionManager;

  /**
   * Creates one application service with the required runtime collaborators.
   *
   * @param agentRunGateway event-driven run gateway
   * @param sessionManager session manager used to load or create sessions
   */
  public AgentRunApplicationService(AgentRunGateway agentRunGateway, SessionManager sessionManager) {
    this.agentRunGateway = Objects.requireNonNull(agentRunGateway, "agentRunGateway must not be null");
    this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager must not be null");
  }

  /**
   * Creates and starts one agent run.
   *
   * @param request create-run request payload
   * @param observer observer that receives emitted run events
   * @return latest run snapshot
   */
  public AgentRunSnapshot createRun(AgentRunCreateRequest request, AgentRunObserver observer) {
    AgentRunCreateRequest nonNullRequest = Objects.requireNonNull(request, "request must not be null");
    AgentRunObserver nonNullObserver = observer == null ? AgentRunObserver.NOOP : observer;
    try {
      String sessionId = resolveSessionId(nonNullRequest);
      AgentTask task = new AgentTask(
          nonNullRequest.taskId(),
          sessionId,
          nonNullRequest.spaceId(),
          Path.of(nonNullRequest.workspaceRoot()),
          TaskMode.valueOf(nonNullRequest.mode()),
          nonNullRequest.intent(),
          nonNullRequest.targetAgentId(),
          nonNullRequest.metadata());
      return agentRunGateway.start(task, nonNullObserver);
    } catch (IllegalArgumentException ex) {
      throw invalidRequest(messageOrFallback(ex, "invalid create request"), ex);
    } catch (AgentExecutionException ex) {
      throw invalidRequest(messageOrFallback(ex, "agent run request failed"), ex);
    }
  }

  /**
   * Loads one run snapshot by identifier.
   *
   * @param runId run identifier
   * @return latest run snapshot
   */
  public AgentRunSnapshot getRun(String runId) {
    try {
      return agentRunGateway.get(ApiModelSupport.requireText(runId, "runId"));
    } catch (IllegalArgumentException ex) {
      throw invalidRequest(messageOrFallback(ex, "invalid run identifier"), ex);
    } catch (AgentExecutionException ex) {
      throw new AgentApiException(
          404,
          new ErrorResponse("AGENT_RUN_NOT_FOUND", messageOrFallback(ex, "run not found")),
          ex);
    }
  }

  /**
   * Resumes one paused run with user feedback.
   *
   * @param runId run identifier
   * @param request feedback request payload
   * @param observer observer that receives emitted run events
   * @return latest run snapshot
   */
  public AgentRunSnapshot resumeRun(String runId, AgentRunFeedbackRequest request, AgentRunObserver observer) {
    try {
      AgentRunObserver nonNullObserver = observer == null ? AgentRunObserver.NOOP : observer;
      return agentRunGateway.resume(
          ApiModelSupport.requireText(runId, "runId"),
          toTransition(Objects.requireNonNull(request, "request must not be null")),
          nonNullObserver);
    } catch (IllegalArgumentException ex) {
      throw invalidRequest(messageOrFallback(ex, "invalid feedback request"), ex);
    } catch (AgentExecutionException ex) {
      throw mutationException(ex, "AGENT_RUN_RESUME_FAILED");
    }
  }

  /**
   * Cancels one run.
   *
   * @param runId run identifier
   * @param request cancel request payload
   * @param observer observer that receives emitted run events
   * @return latest run snapshot
   */
  public AgentRunSnapshot cancelRun(String runId, AgentRunCancelRequest request, AgentRunObserver observer) {
    try {
      AgentRunObserver nonNullObserver = observer == null ? AgentRunObserver.NOOP : observer;
      return agentRunGateway.cancel(
          ApiModelSupport.requireText(runId, "runId"),
          Objects.requireNonNull(request, "request must not be null").reason(),
          nonNullObserver);
    } catch (IllegalArgumentException ex) {
      throw invalidRequest(messageOrFallback(ex, "invalid cancel request"), ex);
    } catch (AgentExecutionException ex) {
      throw mutationException(ex, "AGENT_RUN_CANCEL_FAILED");
    }
  }

  private String resolveSessionId(AgentRunCreateRequest request) {
    if (request.sessionId() != null) {
      return request.sessionId();
    }
    if (request.spaceId() == null) {
      throw new IllegalArgumentException("spaceId must not be blank when sessionId is absent");
    }
    Session session = sessionManager.create(new SessionDraft(
        generatedSessionId(),
        sessionTitleFor(request.intent()),
        request.spaceId(),
        Map.of(
            "source", "agent-run-api",
            "taskId", request.taskId())));
    return session.id();
  }

  private static String generatedSessionId() {
    return "session-" + UUID.randomUUID();
  }

  private static String sessionTitleFor(String intent) {
    String normalizedIntent = intent == null || intent.isBlank() ? "Agent Run" : intent.trim();
    return normalizedIntent.length() <= 80 ? normalizedIntent : normalizedIntent.substring(0, 80);
  }

  private static AgentApiException mutationException(AgentExecutionException ex, String fallbackCode) {
    String message = messageOrFallback(ex, "agent run mutation failed");
    if (message.contains("run not found")) {
      return new AgentApiException(404, new ErrorResponse("AGENT_RUN_NOT_FOUND", message), ex);
    }
    if (message.contains("awaiting") || message.contains("terminal")) {
      return new AgentApiException(409, new ErrorResponse("AGENT_RUN_STATE_CONFLICT", message), ex);
    }
    return new AgentApiException(409, new ErrorResponse(fallbackCode, message), ex);
  }

  private static AgentApiException invalidRequest(String message, Throwable cause) {
    return new AgentApiException(400, new ErrorResponse("AGENT_RUN_REQUEST_INVALID", message), cause);
  }

  private static AgentRunTransition toTransition(AgentRunFeedbackRequest request) {
    return new AgentRunTransition(
        request.content(),
        request.nextAgentId(),
        parseRole(request.nextRole()),
        request.complete());
  }

  private static AgentRole parseRole(String value) {
    return value == null ? null : AgentRole.valueOf(value);
  }

  private static String messageOrFallback(Throwable throwable, String fallback) {
    return throwable.getMessage() == null || throwable.getMessage().isBlank() ? fallback : throwable.getMessage();
  }
}
