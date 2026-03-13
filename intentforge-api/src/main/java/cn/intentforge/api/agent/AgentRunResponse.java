package cn.intentforge.api.agent;

import cn.intentforge.api.util.ApiModelSupport;

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
    List<AgentRunEventResponse> events
) {
  /**
   * Creates a validated run response payload.
   */
  public AgentRunResponse {
    runId = ApiModelSupport.requireText(runId, "runId");
    taskId = ApiModelSupport.requireText(taskId, "taskId");
    sessionId = ApiModelSupport.requireText(sessionId, "sessionId");
    status = ApiModelSupport.requireText(status, "status");
    if (nextStepIndex < 0) {
      throw new IllegalArgumentException("nextStepIndex must not be negative");
    }
    awaitingReason = ApiModelSupport.normalize(awaitingReason);
    eventsPath = ApiModelSupport.requireText(eventsPath, "eventsPath");
    selectedRuntimes = ApiModelSupport.immutableList(selectedRuntimes, "selectedRuntimes");
    events = ApiModelSupport.immutableList(events, "events");
  }
}
