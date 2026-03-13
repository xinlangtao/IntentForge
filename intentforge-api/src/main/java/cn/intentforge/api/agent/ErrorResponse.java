package cn.intentforge.api.agent;


/**
 * Minimal error payload returned by the HTTP API.
 *
 * @param code stable error code
 * @param message human-readable error message
 */
public record ErrorResponse(String code, String message) {
  /**
   * Creates a validated error payload.
   */
  public ErrorResponse {
    code = cn.intentforge.common.util.ValidationSupport.requireText(code, "code");
    message = cn.intentforge.common.util.ValidationSupport.requireText(message, "message");
  }
}
