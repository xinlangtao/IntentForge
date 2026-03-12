package cn.intentforge.tool.core.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shell details detected for the current runtime.
 *
 * @param executable shell executable or command
 * @param source source of the detected shell value
 * @param loginShellPreferred whether login-shell mode should be preferred
 */
public record ToolShellEnvironment(
    String executable,
    String source,
    boolean loginShellPreferred
) {
  /**
   * Creates shell environment details.
   *
   * @param executable shell executable or command
   * @param source source of the detected shell value
   * @param loginShellPreferred whether login-shell mode should be preferred
   */
  public ToolShellEnvironment {
    executable = normalize(executable, "unknown");
    source = normalize(source, "unknown");
  }

  /**
   * Converts shell details to a structured map.
   *
   * @return structured shell details
   */
  public Map<String, Object> toMap() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("executable", executable);
    result.put("source", source);
    result.put("loginShellPreferred", loginShellPreferred);
    return Map.copyOf(result);
  }

  private static String normalize(String value, String fallback) {
    if (value == null) {
      return fallback;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? fallback : normalized;
  }
}
