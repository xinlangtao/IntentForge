package cn.intentforge.api.agent;


import java.util.List;

/**
 * Latest run snapshot returned by the minimal HTTP API.
 *
 * @param runId stable run identifier
 * @param taskId task identifier associated with the run
 * @param sessionId session identifier associated with the run
 * @param status current run status name
 * @param nextStepIndex zero-based index of the next stage to execute
 * @param awaitingReason optional waiting reason
 * @param eventsPath relative SSE path for run events
 * @param selectedRuntimes runtime implementations selected for the current run
 * @param selectedRouteSteps selected route steps already chosen for this run
 * @param availableNextActions user-selectable next actions visible at the current checkpoint
 * @param events ordered run events known at response time
 */
public record AgentRunResponse(
    String runId,
    String taskId,
    String sessionId,
    String status,
    int nextStepIndex,
    String awaitingReason,
    String eventsPath,
    List<RuntimeImplementationResponse> selectedRuntimes,
    List<AgentRouteStepResponse> selectedRouteSteps,
    List<AgentRunActionResponse> availableNextActions,
    List<AgentRunEventResponse> events
) {
  /**
   * Creates a validated run response payload.
   */
  public AgentRunResponse {
    runId = cn.intentforge.common.util.ValidationSupport.requireText(runId, "runId");
    taskId = cn.intentforge.common.util.ValidationSupport.requireText(taskId, "taskId");
    sessionId = cn.intentforge.common.util.ValidationSupport.requireText(sessionId, "sessionId");
    status = cn.intentforge.common.util.ValidationSupport.requireText(status, "status");
    if (nextStepIndex < 0) {
      throw new IllegalArgumentException("nextStepIndex must not be negative");
    }
    awaitingReason = cn.intentforge.common.util.ValidationSupport.normalize(awaitingReason);
    eventsPath = cn.intentforge.common.util.ValidationSupport.requireText(eventsPath, "eventsPath");
    selectedRuntimes = cn.intentforge.common.util.ValidationSupport.immutableList(selectedRuntimes, "selectedRuntimes");
    selectedRouteSteps = cn.intentforge.common.util.ValidationSupport.immutableList(selectedRouteSteps, "selectedRouteSteps");
    availableNextActions = cn.intentforge.common.util.ValidationSupport.immutableList(availableNextActions, "availableNextActions");
    events = cn.intentforge.common.util.ValidationSupport.immutableList(events, "events");
  }
}
