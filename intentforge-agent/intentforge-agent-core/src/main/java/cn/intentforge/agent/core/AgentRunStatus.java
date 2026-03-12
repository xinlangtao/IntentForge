package cn.intentforge.agent.core;

/**
 * Lifecycle state of one event-driven agent run.
 */
public enum AgentRunStatus {
  /**
   * Run is actively executing routed stages.
   */
  RUNNING,
  /**
   * Run is waiting for additional user feedback before continuing.
   */
  AWAITING_USER,
  /**
   * Run finished all routed stages successfully.
   */
  COMPLETED,
  /**
   * Run was cancelled before completion.
   */
  CANCELLED,
  /**
   * Run failed because of an execution error.
   */
  FAILED;

  /**
   * Returns whether the status is terminal.
   *
   * @return {@code true} when the run can no longer continue
   */
  public boolean isTerminal() {
    return this == COMPLETED || this == CANCELLED || this == FAILED;
  }
}
