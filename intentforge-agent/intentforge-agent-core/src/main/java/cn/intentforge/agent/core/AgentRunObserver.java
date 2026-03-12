package cn.intentforge.agent.core;

/**
 * Receives transport-agnostic run events as they are emitted.
 */
@FunctionalInterface
public interface AgentRunObserver {
  /**
   * No-op observer instance.
   */
  AgentRunObserver NOOP = event -> {
  };

  /**
   * Accepts one run event.
   *
   * @param event emitted event
   */
  void onEvent(AgentRunEvent event);
}
