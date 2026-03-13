package cn.intentforge.agent.core;

/**
 * Entry point used to start, observe, resume, and cancel event-driven agent runs.
 */
public interface AgentRunGateway {
  /**
   * Starts one run without an external observer.
   *
   * @param task task request
   * @return latest run snapshot
   */
  default AgentRunSnapshot start(AgentTask task) {
    return start(task, AgentRunObserver.NOOP);
  }

  /**
   * Starts one run and forwards emitted events to the observer.
   *
   * @param task task request
   * @param observer run observer
   * @return latest run snapshot
   */
  AgentRunSnapshot start(AgentTask task, AgentRunObserver observer);

  /**
   * Loads one run snapshot by identifier.
   *
   * @param runId run identifier
   * @return latest run snapshot
   */
  AgentRunSnapshot get(String runId);

  /**
   * Resumes one paused run without an external observer.
   *
   * @param runId run identifier
   * @param feedback optional user feedback
   * @return latest run snapshot
   */
  default AgentRunSnapshot resume(String runId, String feedback) {
    return resume(runId, feedback, AgentRunObserver.NOOP);
  }

  /**
   * Resumes one paused run with an explicit transition selection and without an external observer.
   *
   * @param runId run identifier
   * @param transition explicit transition selection
   * @return latest run snapshot
   */
  default AgentRunSnapshot resume(String runId, AgentRunTransition transition) {
    return resume(runId, transition, AgentRunObserver.NOOP);
  }

  /**
   * Resumes one paused run and forwards emitted events to the observer.
   *
   * @param runId run identifier
   * @param feedback optional user feedback
   * @param observer run observer
   * @return latest run snapshot
   */
  AgentRunSnapshot resume(String runId, String feedback, AgentRunObserver observer);

  /**
   * Resumes one paused run with an explicit transition selection and forwards emitted events to the observer.
   *
   * @param runId run identifier
   * @param transition explicit transition selection
   * @param observer run observer
   * @return latest run snapshot
   */
  AgentRunSnapshot resume(String runId, AgentRunTransition transition, AgentRunObserver observer);

  /**
   * Cancels one run without an external observer.
   *
   * @param runId run identifier
   * @param reason cancellation reason
   * @return latest run snapshot
   */
  default AgentRunSnapshot cancel(String runId, String reason) {
    return cancel(runId, reason, AgentRunObserver.NOOP);
  }

  /**
   * Cancels one run and forwards emitted events to the observer.
   *
   * @param runId run identifier
   * @param reason cancellation reason
   * @param observer run observer
   * @return latest run snapshot
   */
  AgentRunSnapshot cancel(String runId, String reason, AgentRunObserver observer);
}
