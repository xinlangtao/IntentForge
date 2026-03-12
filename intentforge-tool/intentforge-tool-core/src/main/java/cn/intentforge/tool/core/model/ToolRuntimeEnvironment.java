package cn.intentforge.tool.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalized host environment information shared by tool invocations.
 *
 * @param operatingSystemFamily normalized operating-system family
 * @param operatingSystemName raw operating-system name
 * @param operatingSystemVersion operating-system version
 * @param architecture CPU architecture
 * @param shell detected shell details
 * @param terminal detected terminal details
 * @param ideLaunchers detected IDE launch entries for the current workspace
 */
public record ToolRuntimeEnvironment(
    String operatingSystemFamily,
    String operatingSystemName,
    String operatingSystemVersion,
    String architecture,
    ToolShellEnvironment shell,
    ToolTerminalEnvironment terminal,
    List<ToolIdeLauncher> ideLaunchers
) {
  /**
   * Creates runtime environment details.
   *
   * @param operatingSystemFamily normalized operating-system family
   * @param operatingSystemName raw operating-system name
   * @param operatingSystemVersion operating-system version
   * @param architecture CPU architecture
   * @param shell detected shell details
   * @param terminal detected terminal details
   * @param ideLaunchers detected IDE launch entries for the current workspace
   */
  public ToolRuntimeEnvironment {
    operatingSystemFamily = normalize(operatingSystemFamily, "other");
    operatingSystemName = normalize(operatingSystemName, "unknown");
    operatingSystemVersion = normalize(operatingSystemVersion, "unknown");
    architecture = normalize(architecture, "unknown");
    shell = shell == null ? new ToolShellEnvironment("unknown", "unknown", true) : shell;
    terminal = terminal == null ? new ToolTerminalEnvironment("unknown", "unknown", "unknown", "unknown", "unknown") : terminal;
    ideLaunchers = immutableLaunchers(ideLaunchers);
  }

  /**
   * Converts runtime environment details to a structured map.
   *
   * @return structured runtime environment details
   */
  public Map<String, Object> toMap() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("operatingSystemFamily", operatingSystemFamily);
    result.put("operatingSystemName", operatingSystemName);
    result.put("operatingSystemVersion", operatingSystemVersion);
    result.put("architecture", architecture);
    result.put("shell", shell.toMap());
    result.put("terminal", terminal.toMap());
    result.put("ideLaunchers", ideLaunchers.stream().map(ToolIdeLauncher::toMap).toList());
    return Map.copyOf(result);
  }

  private static String normalize(String value, String fallback) {
    if (value == null) {
      return fallback;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? fallback : normalized;
  }

  private static List<ToolIdeLauncher> immutableLaunchers(List<ToolIdeLauncher> values) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    List<ToolIdeLauncher> normalized = new ArrayList<>();
    for (ToolIdeLauncher value : values) {
      if (value != null) {
        normalized.add(value);
      }
    }
    return List.copyOf(normalized);
  }
}
