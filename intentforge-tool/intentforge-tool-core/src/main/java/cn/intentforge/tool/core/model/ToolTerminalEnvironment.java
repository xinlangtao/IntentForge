package cn.intentforge.tool.core.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Terminal details detected for the current runtime.
 *
 * @param program terminal program identifier
 * @param type terminal type identifier such as {@code xterm-256color}
 * @param version terminal program version
 * @param sessionId terminal session identifier
 * @param host detected host IDE or terminal family
 */
public record ToolTerminalEnvironment(
    String program,
    String type,
    String version,
    String sessionId,
    String host
) {
  /**
   * Creates terminal environment details.
   *
   * @param program terminal program identifier
   * @param type terminal type identifier such as {@code xterm-256color}
   * @param version terminal program version
   * @param sessionId terminal session identifier
   * @param host detected host IDE or terminal family
   */
  public ToolTerminalEnvironment {
    program = normalize(program);
    type = normalize(type);
    version = normalize(version);
    sessionId = normalize(sessionId);
    host = normalize(host);
  }

  /**
   * Converts terminal details to a structured map.
   *
   * @return structured terminal details
   */
  public Map<String, Object> toMap() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("program", program);
    result.put("type", type);
    result.put("version", version);
    result.put("sessionId", sessionId);
    result.put("host", host);
    return Map.copyOf(result);
  }

  private static String normalize(String value) {
    if (value == null) {
      return "unknown";
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? "unknown" : normalized;
  }
}
