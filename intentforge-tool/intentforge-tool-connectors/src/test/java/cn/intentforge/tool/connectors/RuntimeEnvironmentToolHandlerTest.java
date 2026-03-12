package cn.intentforge.tool.connectors;

import cn.intentforge.tool.core.model.ToolCallRequest;
import cn.intentforge.tool.core.model.ToolCallStatus;
import cn.intentforge.tool.core.model.ToolExecutionContext;
import cn.intentforge.tool.core.model.ToolIdeLauncher;
import cn.intentforge.tool.core.model.ToolRuntimeEnvironment;
import cn.intentforge.tool.core.model.ToolShellEnvironment;
import cn.intentforge.tool.core.model.ToolTerminalEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RuntimeEnvironmentToolHandlerTest {
  @Test
  void shouldReadEnvironmentFromContext() throws Exception {
    Path workspace = Files.createTempDirectory("runtime-environment-tool");
    ToolRuntimeEnvironment environment = new ToolRuntimeEnvironment(
        "macos",
        "Mac OS X",
        "14.5",
        "aarch64",
        new ToolShellEnvironment("/bin/zsh", "env:SHELL", true),
        new ToolTerminalEnvironment("vscode", "xterm-256color", "1.99.0", "session-1", "vscode"),
        List.of(new ToolIdeLauncher(
            "vscode",
            "Visual Studio Code",
            "/usr/local/bin/code",
            "/usr/local/bin/code '" + workspace + "'",
            "PATH")));
    ToolExecutionContext context = ToolExecutionContext.of(
        workspace,
        workspace.resolve(".intentforge").resolve("tool-output"),
        environment,
        Map.of());

    RuntimeEnvironmentToolHandler handler = new RuntimeEnvironmentToolHandler();
    var result = handler.handle(new ToolCallRequest(ConnectorToolPlugin.TOOL_RUNTIME_ENVIRONMENT_READ, Map.of(), context));

    Assertions.assertEquals(ToolCallStatus.SUCCESS, result.status());
    Assertions.assertTrue(result.output().contains("macos"));
    Assertions.assertTrue(result.structured() instanceof Map<?, ?>);
    Map<?, ?> structured = (Map<?, ?>) result.structured();
    Assertions.assertEquals("macos", structured.get("operatingSystemFamily"));
    Assertions.assertTrue(structured.get("shell") instanceof Map<?, ?>);
    Map<?, ?> shell = (Map<?, ?>) structured.get("shell");
    Assertions.assertEquals("/bin/zsh", shell.get("executable"));
  }
}
