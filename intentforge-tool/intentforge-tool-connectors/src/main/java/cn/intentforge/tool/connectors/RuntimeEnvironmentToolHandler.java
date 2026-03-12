package cn.intentforge.tool.connectors;

import cn.intentforge.tool.core.ToolHandler;
import cn.intentforge.tool.core.model.ToolCallRequest;
import cn.intentforge.tool.core.model.ToolCallResult;
import cn.intentforge.tool.core.model.ToolIdeLauncher;
import cn.intentforge.tool.core.model.ToolRuntimeEnvironment;
import java.util.Map;

/**
 * Handler for {@code intentforge.runtime.environment.read}.
 */
public final class RuntimeEnvironmentToolHandler implements ToolHandler {
  /**
   * Reads the normalized runtime environment from tool context.
   *
   * @param request tool request
   * @return execution result containing host environment details
   */
  @Override
  public ToolCallResult handle(ToolCallRequest request) {
    ToolRuntimeEnvironment environment = request.context().runtimeEnvironment();
    Map<String, Object> metadata = Map.of(
        "workspaceRoot", request.context().workspaceRoot().toString(),
        "ideLauncherCount", environment.ideLaunchers().size());
    return ToolCallResult.success(buildOutput(environment), environment.toMap(), metadata);
  }

  private static String buildOutput(ToolRuntimeEnvironment environment) {
    StringBuilder output = new StringBuilder();
    output.append("os=")
        .append(environment.operatingSystemFamily())
        .append(' ')
        .append(environment.operatingSystemName())
        .append(' ')
        .append(environment.operatingSystemVersion())
        .append(" arch=")
        .append(environment.architecture())
        .append('\n');
    output.append("shell=")
        .append(environment.shell().executable())
        .append(" source=")
        .append(environment.shell().source())
        .append('\n');
    output.append("terminal=")
        .append(environment.terminal().program())
        .append(" type=")
        .append(environment.terminal().type())
        .append(" host=")
        .append(environment.terminal().host());
    if (!environment.ideLaunchers().isEmpty()) {
      output.append('\n').append("ideLaunchers:");
      for (ToolIdeLauncher launcher : environment.ideLaunchers()) {
        output.append('\n')
            .append("- ")
            .append(launcher.displayName())
            .append(": ")
            .append(launcher.launchCommand());
      }
    }
    return output.toString();
  }
}
