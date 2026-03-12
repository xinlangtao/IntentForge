package cn.intentforge.session;

/**
 * Raised when a requested session cannot be found in the current manager.
 *
 * <p>This exception is immutable and thread-safe.
 */
public final class SessionNotFoundException extends RuntimeException {
  /**
   * Creates the exception with a detail message.
   *
   * @param message detail message
   */
  public SessionNotFoundException(String message) {
    super(message);
  }
}
