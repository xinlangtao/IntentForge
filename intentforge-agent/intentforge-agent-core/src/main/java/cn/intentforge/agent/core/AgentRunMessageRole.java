package cn.intentforge.agent.core;

/**
 * Declares the role of one message inside an event-driven agent run.
 */
public enum AgentRunMessageRole {
  /**
   * End-user feedback or follow-up instruction.
   */
  USER,
  /**
   * Agent-produced message or note.
   */
  AGENT,
  /**
   * System or runtime-generated message.
   */
  SYSTEM
}
