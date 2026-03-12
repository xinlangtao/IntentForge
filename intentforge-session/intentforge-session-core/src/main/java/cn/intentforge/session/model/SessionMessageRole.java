package cn.intentforge.session.model;

/**
 * Author role for one session message.
 *
 * <p>Enum values are immutable and thread-safe.
 */
public enum SessionMessageRole {
  /**
   * System or runtime level instruction.
   */
  SYSTEM,
  /**
   * End-user authored input.
   */
  USER,
  /**
   * Assistant authored response.
   */
  ASSISTANT,
  /**
   * Tool or external system output.
   */
  TOOL
}
