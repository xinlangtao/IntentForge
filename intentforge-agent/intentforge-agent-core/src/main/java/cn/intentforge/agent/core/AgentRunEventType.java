package cn.intentforge.agent.core;

/**
 * Event types emitted during the lifecycle of one agent run.
 */
public enum AgentRunEventType {
  /**
   * Run object was created.
   */
  RUN_CREATED,
  /**
   * Runtime context was resolved.
   */
  CONTEXT_RESOLVED,
  /**
   * Governance selected a route.
   */
  ROUTE_SELECTED,
  /**
   * One routed stage started.
   */
  STAGE_STARTED,
  /**
   * One routed stage completed.
   */
  STAGE_COMPLETED,
  /**
   * User feedback was recorded.
   */
  USER_FEEDBACK_RECEIVED,
  /**
   * Run resumed after a pause.
   */
  RUN_RESUMED,
  /**
   * Run paused and is awaiting user input.
   */
  AWAITING_USER,
  /**
   * Run finished successfully.
   */
  RUN_COMPLETED,
  /**
   * Run was cancelled.
   */
  RUN_CANCELLED,
  /**
   * Run failed unexpectedly.
   */
  RUN_FAILED
}
