package cn.intentforge.session.model;

/**
 * Lifecycle status for a session snapshot.
 *
 * <p>Enum values are immutable and thread-safe.
 */
public enum SessionStatus {
  /**
   * Session accepts new messages and remains queryable as active work.
   */
  ACTIVE,
  /**
   * Session is retained for history but should no longer accept normal updates.
   */
  ARCHIVED
}
