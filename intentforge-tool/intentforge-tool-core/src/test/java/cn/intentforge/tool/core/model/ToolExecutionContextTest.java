package cn.intentforge.tool.core.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ToolExecutionContextTest {
  @Test
  void shouldAttachRuntimeEnvironmentWhenContextIsCreated() throws Exception {
    ToolExecutionContext context = ToolExecutionContext.create(Files.createTempDirectory("tool-context"));

    Assertions.assertNotNull(context.runtimeEnvironment());
    Assertions.assertFalse(context.runtimeEnvironment().operatingSystemFamily().isBlank());
    Assertions.assertFalse(context.runtimeEnvironment().shell().executable().isBlank());
    Assertions.assertNotNull(context.runtimeEnvironment().terminal());
  }

  @Test
  void shouldAllowExplicitRuntimeEnvironmentOverride() throws Exception {
    Path workspace = Files.createTempDirectory("tool-context-workspace");
    Path output = Files.createTempDirectory("tool-context-output");
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

    ToolExecutionContext context = ToolExecutionContext.of(workspace, output, environment, Map.of("k", "v"));

    Assertions.assertEquals(environment, context.runtimeEnvironment());
    Assertions.assertEquals("v", context.attribute("k").orElseThrow());
  }
}
