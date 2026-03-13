package cn.intentforge.api.agent;

import cn.intentforge.agent.core.AgentRunEvent;
import cn.intentforge.agent.core.AgentRunAvailableAction;
import cn.intentforge.agent.core.AgentRunObserver;
import cn.intentforge.agent.core.AgentRunSnapshot;
import cn.intentforge.agent.core.AgentRouteStep;
import java.util.List;
import java.util.Objects;

/**
 * Transport-neutral controller that maps agent run use cases to API DTOs.
 */
public final class AgentRunController {
  private final AgentRunApplicationService applicationService;

  /**
   * Creates one controller with the supplied application service.
   *
   * @param applicationService use-case service behind the controller
   */
  public AgentRunController(AgentRunApplicationService applicationService) {
    this.applicationService = Objects.requireNonNull(applicationService, "applicationService must not be null");
  }

  /**
   * Creates and starts one run, then maps the latest snapshot to the API response.
   *
   * @param request create-run request payload
   * @param observer observer that receives emitted run events
   * @return latest run response
   */
  public AgentRunResponse createRun(AgentRunCreateRequest request, AgentRunObserver observer) {
    return toResponse(applicationService.createRun(request, observer));
  }

  /**
   * Loads one run snapshot for transports that need direct event replay access.
   *
   * @param runId run identifier
   * @return latest run snapshot
   */
  public AgentRunSnapshot getRunSnapshot(String runId) {
    return applicationService.getRun(runId);
  }

  /**
   * Resumes one paused run and maps the latest snapshot to the API response.
   *
   * @param runId run identifier
   * @param request feedback request payload
   * @param observer observer that receives emitted run events
   * @return latest run response
   */
  public AgentRunResponse resumeRun(String runId, AgentRunFeedbackRequest request, AgentRunObserver observer) {
    return toResponse(applicationService.resumeRun(runId, request, observer));
  }

  /**
   * Cancels one run and maps the latest snapshot to the API response.
   *
   * @param runId run identifier
   * @param request cancel request payload
   * @param observer observer that receives emitted run events
   * @return latest run response
   */
  public AgentRunResponse cancelRun(String runId, AgentRunCancelRequest request, AgentRunObserver observer) {
    return toResponse(applicationService.cancelRun(runId, request, observer));
  }

  /**
   * Maps one domain run event to the transport DTO used by HTTP responses and SSE data blocks.
   *
   * @param event run event
   * @return mapped event response
   */
  public AgentRunEventResponse toEventResponse(AgentRunEvent event) {
    AgentRunEvent nonNullEvent = Objects.requireNonNull(event, "event must not be null");
    return new AgentRunEventResponse(
        nonNullEvent.runId(),
        nonNullEvent.sequence(),
        nonNullEvent.type().name(),
        nonNullEvent.status().name(),
        nonNullEvent.message(),
        nonNullEvent.metadata(),
        nonNullEvent.createdAt().toString());
  }

  private AgentRunResponse toResponse(AgentRunSnapshot snapshot) {
    AgentRunSnapshot nonNullSnapshot = Objects.requireNonNull(snapshot, "snapshot must not be null");
    return new AgentRunResponse(
        nonNullSnapshot.runId(),
        nonNullSnapshot.task().id(),
        nonNullSnapshot.task().sessionId(),
        nonNullSnapshot.status().name(),
        nonNullSnapshot.nextStepIndex(),
        nonNullSnapshot.awaitingReason(),
        "/api/agent-runs/" + nonNullSnapshot.runId() + "/events",
        nonNullSnapshot.contextPack().runtimeSelection().implementations().values().stream()
            .map(descriptor -> new RuntimeImplementationResponse(
                descriptor.id(),
                descriptor.capability().name(),
                descriptor.displayName(),
                descriptor.implementationClass(),
                descriptor.metadata()))
            .toList(),
        toRouteStepResponses(nonNullSnapshot.route().steps()),
        toActionResponses(nonNullSnapshot.availableNextActions()),
        toEventResponses(nonNullSnapshot.events()));
  }

  private List<AgentRunEventResponse> toEventResponses(List<AgentRunEvent> events) {
    return Objects.requireNonNull(events, "events must not be null").stream()
        .map(this::toEventResponse)
        .toList();
  }

  private List<AgentRouteStepResponse> toRouteStepResponses(List<AgentRouteStep> steps) {
    return Objects.requireNonNull(steps, "steps must not be null").stream()
        .map(step -> new AgentRouteStepResponse(
            step.order(),
            step.agentId(),
            step.role().name(),
            step.reason()))
        .toList();
  }

  private List<AgentRunActionResponse> toActionResponses(List<AgentRunAvailableAction> actions) {
    return Objects.requireNonNull(actions, "actions must not be null").stream()
        .map(action -> new AgentRunActionResponse(
            action.agentId(),
            action.role() == null ? null : action.role().name(),
            action.preferred(),
            action.complete(),
            action.reason()))
        .toList();
  }
}
