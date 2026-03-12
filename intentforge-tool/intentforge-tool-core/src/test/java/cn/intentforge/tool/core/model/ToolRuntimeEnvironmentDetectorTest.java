package cn.intentforge.tool.core.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ToolRuntimeEnvironmentDetectorTest {
  @Test
  void shouldDetectTerminalAndIdeLaunchersFromSnapshot() throws Exception {
    Path workspace = Files.createTempDirectory("tool-env-workspace");
    Path binDirectory = Files.createTempDirectory("tool-env-bin");
    Path codePath = createExecutable(binDirectory, "code");
    Path ideaPath = createExecutable(binDirectory, "idea");

    ToolRuntimeEnvironment environment = ToolRuntimeEnvironmentDetector.detect(
        workspace,
        Map.of(
            "os.name", "Mac OS X",
            "os.version", "14.5",
            "os.arch", "aarch64"),
        Map.of(
            "SHELL", "/bin/zsh",
            "PATH", binDirectory.toString(),
            "TERM_PROGRAM", "vscode",
            "TERM_PROGRAM_VERSION", "1.99.0",
            "TERM", "xterm-256color",
            "TERM_SESSION_ID", "session-1",
            "VSCODE_GIT_IPC_HANDLE", "present"));

    Assertions.assertEquals("macos", environment.operatingSystemFamily());
    Assertions.assertEquals("/bin/zsh", environment.shell().executable());
    Assertions.assertEquals("env:SHELL", environment.shell().source());
    Assertions.assertEquals("vscode", environment.terminal().program());
    Assertions.assertEquals("vscode", environment.terminal().host());
    Assertions.assertEquals(2, environment.ideLaunchers().size());

    ToolIdeLauncher vscode = environment.ideLaunchers().stream()
        .filter(launcher -> launcher.id().equals("vscode"))
        .findFirst()
        .orElseThrow();
    Assertions.assertEquals(codePath.toString(), vscode.executablePath());
    Assertions.assertTrue(vscode.launchCommand().contains(workspace.toString()));

    ToolIdeLauncher idea = environment.ideLaunchers().stream()
        .filter(launcher -> launcher.id().equals("idea"))
        .findFirst()
        .orElseThrow();
    Assertions.assertEquals(ideaPath.toString(), idea.executablePath());
  }

  @Test
  void shouldFallbackWhenSnapshotIsSparseOrBlank() throws Exception {
    ToolRuntimeEnvironment environment = ToolRuntimeEnvironmentDetector.detect(
        Files.createTempDirectory("tool-env-fallback"),
        Map.of(
            "os.name", "Linux",
            "os.version", "6.8",
            "os.arch", "x86_64"),
        Map.of(
            "PATH", "   ",
            "SHELL", "   ",
            "TERM", " "));

    Assertions.assertEquals("linux", environment.operatingSystemFamily());
    Assertions.assertEquals("/bin/bash", environment.shell().executable());
    Assertions.assertEquals("default", environment.shell().source());
    Assertions.assertEquals("unknown", environment.terminal().program());
    Assertions.assertTrue(environment.ideLaunchers().isEmpty());
  }

  private static Path createExecutable(Path directory, String name) throws Exception {
    Path executable = directory.resolve(name);
    Files.writeString(executable, "#!/bin/sh\nexit 0\n");
    Assertions.assertTrue(executable.toFile().setExecutable(true));
    return executable;
  }
}
