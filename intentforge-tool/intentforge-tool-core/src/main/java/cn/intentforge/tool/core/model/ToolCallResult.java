package cn.intentforge.tool.core.model;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified tool execution result.
 *
 * @param status execution status
 * @param output textual output
 * @param structured optional structured output
 * @param metadata optional metadata
 * @param errorCode optional error code
 * @param errorMessage optional error message
 */
public record ToolCallResult(
    ToolCallStatus status,
    String output,
    Object structured,
    Map<String, Object> metadata,
    String errorCode,
    String errorMessage
) {
  /**
   * Creates a tool call result.
   *
   * @param status execution status
   * @param output textual output
   * @param structured structured data
   * @param metadata metadata map
   * @param errorCode error code
   * @param errorMessage error message
   */
  public ToolCallResult {
    if (status == null) {
      throw new IllegalArgumentException("status must not be null");
    }
    output = output == null ? "" : output;
    metadata = immutableMetadata(metadata);
    errorCode = normalize(errorCode);
    errorMessage = normalize(errorMessage);
  }

  /**
   * Builds a success result with text output.
   *
   * @param output textual output
   * @return success result
   */
  public static ToolCallResult success(String output) {
    return new ToolCallResult(ToolCallStatus.SUCCESS, output, null, Map.of(), null, null);
  }

  /**
   * Builds a success result with structured payload.
   *
   * @param output textual output
   * @param structured structured payload
   * @param metadata metadata map
   * @return success result
   */
  public static ToolCallResult success(String output, Object structured, Map<String, Object> metadata) {
    return new ToolCallResult(ToolCallStatus.SUCCESS, output, structured, metadata, null, null);
  }

  /**
   * Builds an error result.
   *
   * @param errorCode error code
   * @param errorMessage error message
   * @return error result
   */
  public static ToolCallResult error(String errorCode, String errorMessage) {
    return new ToolCallResult(ToolCallStatus.ERROR, "", null, Map.of(), errorCode, errorMessage);
  }

  /**
   * Builds an error result with metadata.
   *
   * @param errorCode error code
   * @param errorMessage error message
   * @param metadata metadata
   * @return error result
   */
  public static ToolCallResult error(String errorCode, String errorMessage, Map<String, Object> metadata) {
    return new ToolCallResult(ToolCallStatus.ERROR, "", null, metadata, errorCode, errorMessage);
  }

  /**
   * Builds a suspended result.
   *
   * @param output textual output
   * @param metadata metadata
   * @return suspended result
   */
  public static ToolCallResult suspended(String output, Map<String, Object> metadata) {
    return new ToolCallResult(ToolCallStatus.SUSPENDED, output, null, metadata, null, null);
  }

  private static Map<String, Object> immutableMetadata(Map<String, Object> value) {
    if (value == null || value.isEmpty()) {
      return Map.of();
    }
    return Map.copyOf(new LinkedHashMap<>(value));
  }
}
