package cn.intentforge.tool.core.model;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One detected IDE launch entry for the current workspace.
 *
 * @param id stable IDE identifier
 * @param displayName human-readable IDE name
 * @param executablePath resolved executable path
 * @param launchCommand launch command including the current workspace
 * @param source source of the detected entry
 */
public record ToolIdeLauncher(
    String id,
    String displayName,
    String executablePath,
    String launchCommand,
    String source
) {
  /**
   * Creates an IDE launcher entry.
   *
   * @param id stable IDE identifier
   * @param displayName human-readable IDE name
   * @param executablePath resolved executable path
   * @param launchCommand launch command including the current workspace
   * @param source source of the detected entry
   */
  public ToolIdeLauncher {
    id = requireText(id, "id");
    displayName = requireText(displayName, "displayName");
    executablePath = requireText(executablePath, "executablePath");
    launchCommand = requireText(launchCommand, "launchCommand");
    source = requireText(source, "source");
  }

  /**
   * Converts IDE launcher details to a structured map.
   *
   * @return structured IDE launcher details
   */
  public Map<String, Object> toMap() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", id);
    result.put("displayName", displayName);
    result.put("executablePath", executablePath);
    result.put("launchCommand", launchCommand);
    result.put("source", source);
    return Map.copyOf(result);
  }
}
