package cn.intentforge.space;

/**
 * Indicates that a space hierarchy could not be resolved.
 */
public final class SpaceResolutionException extends RuntimeException {
  /**
   * Creates an exception with message only.
   *
   * @param message detail message
   */
  public SpaceResolutionException(String message) {
    super(message);
  }
}
