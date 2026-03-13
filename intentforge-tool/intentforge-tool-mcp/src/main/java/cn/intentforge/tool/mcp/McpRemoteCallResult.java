package cn.intentforge.tool.mcp;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result returned by MCP remote tool call.
 *
 * @param success whether remote call succeeded
 * @param output textual output
 * @param structured optional structured payload
 * @param metadata optional metadata
 * @param errorCode optional error code
 * @param errorMessage optional error message
 */
public record McpRemoteCallResult(
    boolean success,
    String output,
    Object structured,
    Map<String, Object> metadata,
    String errorCode,
    String errorMessage
) {
  /**
   * Creates one remote call result.
   *
   * @param success whether remote call succeeded
   * @param output textual output
   * @param structured optional structured payload
   * @param metadata optional metadata
   * @param errorCode optional error code
   * @param errorMessage optional error message
   */
  public McpRemoteCallResult {
    output = output == null ? "" : output;
    metadata = metadata == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(metadata));
    errorCode = normalize(errorCode);
    errorMessage = normalize(errorMessage);
  }

  /**
   * Creates successful remote result.
   *
   * @param output output text
   * @param structured structured payload
   * @param metadata metadata payload
   * @return successful remote result
   */
  public static McpRemoteCallResult success(String output, Object structured, Map<String, Object> metadata) {
    return new McpRemoteCallResult(true, output, structured, metadata, null, null);
  }

  /**
   * Creates failed remote result.
   *
   * @param errorCode error code
   * @param errorMessage error message
   * @return failed remote result
   */
  public static McpRemoteCallResult error(String errorCode, String errorMessage) {
    return new McpRemoteCallResult(false, "", null, Map.of(), errorCode, errorMessage);
  }
}
