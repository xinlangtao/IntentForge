package cn.intentforge.hook;

/**
 * Signals one hook endpoint request failure that should be translated to an HTTP response.
 *
 * @since 1.0.0
 */
final class HookEndpointException extends RuntimeException {
  private final int statusCode;

  HookEndpointException(int statusCode, String message) {
    super(message);
    this.statusCode = statusCode;
  }

  int statusCode() {
    return statusCode;
  }
}
