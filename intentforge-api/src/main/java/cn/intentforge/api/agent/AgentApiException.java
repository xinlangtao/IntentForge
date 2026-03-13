package cn.intentforge.api.agent;

import java.util.Objects;

/**
 * Runtime exception that carries one transport-level HTTP status and error payload.
 */
public final class AgentApiException extends RuntimeException {
  private final int statusCode;
  private final ErrorResponse error;

  /**
   * Creates one API exception with the supplied HTTP status and payload.
   *
   * @param statusCode HTTP status code
   * @param error error payload
   */
  public AgentApiException(int statusCode, ErrorResponse error) {
    this(statusCode, error, null);
  }

  /**
   * Creates one API exception with the supplied HTTP status, payload, and cause.
   *
   * @param statusCode HTTP status code
   * @param error error payload
   * @param cause root cause
   */
  public AgentApiException(int statusCode, ErrorResponse error, Throwable cause) {
    super(Objects.requireNonNull(error, "error must not be null").message(), cause);
    if (statusCode < 400 || statusCode > 599) {
      throw new IllegalArgumentException("statusCode must be a 4xx or 5xx response code");
    }
    this.statusCode = statusCode;
    this.error = error;
  }

  /**
   * Returns the HTTP status code that should be returned to the client.
   *
   * @return HTTP status code
   */
  public int statusCode() {
    return statusCode;
  }

  /**
   * Returns the structured error payload.
   *
   * @return error payload
   */
  public ErrorResponse error() {
    return error;
  }
}
