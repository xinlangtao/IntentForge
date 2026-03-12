package cn.intentforge.tool.core.model;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class ToolRuntimeEnvironmentDetector {
  private static final List<IdeCandidate> IDE_CANDIDATES = List.of(
      new IdeCandidate("vscode", "Visual Studio Code", List.of("code", "code-insiders")),
      new IdeCandidate("cursor", "Cursor", List.of("cursor")),
      new IdeCandidate("windsurf", "Windsurf", List.of("windsurf")),
      new IdeCandidate("zed", "Zed", List.of("zed")),
      new IdeCandidate("idea", "IntelliJ IDEA", List.of("idea", "idea64.exe")),
      new IdeCandidate("pycharm", "PyCharm", List.of("pycharm", "pycharm64.exe")),
      new IdeCandidate("webstorm", "WebStorm", List.of("webstorm", "webstorm64.exe")),
      new IdeCandidate("goland", "GoLand", List.of("goland", "goland64.exe")),
      new IdeCandidate("clion", "CLion", List.of("clion", "clion64.exe")));

  private ToolRuntimeEnvironmentDetector() {
  }

  static ToolRuntimeEnvironment detect(Path workspaceRoot) {
    return detect(workspaceRoot, systemProperties(), System.getenv());
  }

  static ToolRuntimeEnvironment detect(
      Path workspaceRoot,
      Map<String, String> systemProperties,
      Map<String, String> environmentVariables
  ) {
    Path normalizedWorkspace = Objects.requireNonNull(workspaceRoot, "workspaceRoot must not be null")
        .toAbsolutePath()
        .normalize();
    Map<String, String> normalizedProperties = normalizeMap(systemProperties);
    Map<String, String> normalizedEnvironment = normalizeMap(environmentVariables);

    String operatingSystemName = valueOrDefault(normalizedProperties.get("os.name"), "unknown");
    String family = detectOperatingSystemFamily(operatingSystemName);
    return new ToolRuntimeEnvironment(
        family,
        operatingSystemName,
        valueOrDefault(normalizedProperties.get("os.version"), "unknown"),
        valueOrDefault(normalizedProperties.get("os.arch"), "unknown"),
        detectShell(family, normalizedEnvironment),
        detectTerminal(normalizedEnvironment),
        detectIdeLaunchers(normalizedWorkspace, family, normalizedEnvironment));
  }

  private static ToolShellEnvironment detectShell(String operatingSystemFamily, Map<String, String> environmentVariables) {
    String shell = normalize(environmentVariables.get("SHELL"));
    if (shell != null) {
      return new ToolShellEnvironment(shell, "env:SHELL", !"windows".equals(operatingSystemFamily));
    }

    String comSpec = normalize(firstNonBlank(environmentVariables.get("COMSPEC"), environmentVariables.get("ComSpec")));
    if (comSpec != null) {
      String source = environmentVariables.containsKey("COMSPEC") ? "env:COMSPEC" : "env:ComSpec";
      return new ToolShellEnvironment(comSpec, source, false);
    }

    String fallback = switch (operatingSystemFamily) {
      case "windows" -> "cmd.exe";
      case "macos" -> "/bin/zsh";
      default -> "/bin/bash";
    };
    return new ToolShellEnvironment(fallback, "default", !"windows".equals(operatingSystemFamily));
  }

  private static ToolTerminalEnvironment detectTerminal(Map<String, String> environmentVariables) {
    String program = firstNonBlank(
        environmentVariables.get("TERM_PROGRAM"),
        environmentVariables.get("TERMINAL_EMULATOR"),
        hasText(environmentVariables.get("WT_SESSION")) ? "Windows Terminal" : null,
        "unknown");
    String type = firstNonBlank(environmentVariables.get("TERM"), "unknown");
    String version = firstNonBlank(
        environmentVariables.get("TERM_PROGRAM_VERSION"),
        environmentVariables.get("TERMINAL_EMULATOR_VERSION"),
        "unknown");
    String sessionId = firstNonBlank(
        environmentVariables.get("TERM_SESSION_ID"),
        environmentVariables.get("WT_SESSION"),
        environmentVariables.get("SESSIONNAME"),
        "unknown");
    return new ToolTerminalEnvironment(program, type, version, sessionId, detectTerminalHost(environmentVariables));
  }

  private static String detectTerminalHost(Map<String, String> environmentVariables) {
    if (hasText(environmentVariables.get("CURSOR_TRACE_ID"))
        || equalsIgnoreCase(environmentVariables.get("TERM_PROGRAM"), "cursor")) {
      return "cursor";
    }
    if (hasText(environmentVariables.get("WINDSURF_TRACE_ID"))
        || equalsIgnoreCase(environmentVariables.get("TERM_PROGRAM"), "windsurf")) {
      return "windsurf";
    }
    if (hasText(environmentVariables.get("ZED_TERM"))
        || equalsIgnoreCase(environmentVariables.get("TERM_PROGRAM"), "zed")) {
      return "zed";
    }
    if (hasText(environmentVariables.get("VSCODE_GIT_IPC_HANDLE"))
        || equalsIgnoreCase(environmentVariables.get("TERM_PROGRAM"), "vscode")) {
      return "vscode";
    }
    if (containsIgnoreCase(environmentVariables.get("TERMINAL_EMULATOR"), "jetbrains")
        || hasText(environmentVariables.get("IDEA_INITIAL_DIRECTORY"))
        || hasText(environmentVariables.get("JETBRAINS_IDE"))) {
      return "jetbrains";
    }
    return "unknown";
  }

  private static List<ToolIdeLauncher> detectIdeLaunchers(
      Path workspaceRoot,
      String operatingSystemFamily,
      Map<String, String> environmentVariables
  ) {
    String rawPath = normalize(environmentVariables.get("PATH"));
    if (rawPath == null) {
      return List.of();
    }

    List<Path> searchDirectories = splitPath(rawPath);
    if (searchDirectories.isEmpty()) {
      return List.of();
    }

    Map<String, ToolIdeLauncher> launchers = new LinkedHashMap<>();
    for (IdeCandidate candidate : IDE_CANDIDATES) {
      for (String executableName : candidate.executableNames()) {
        Path resolvedExecutable = resolveExecutable(searchDirectories, executableName, operatingSystemFamily, environmentVariables);
        if (resolvedExecutable == null) {
          continue;
        }
        launchers.putIfAbsent(
            candidate.id(),
            new ToolIdeLauncher(
                candidate.id(),
                candidate.displayName(),
                resolvedExecutable.toString(),
                buildLaunchCommand(resolvedExecutable, workspaceRoot, operatingSystemFamily),
                "PATH"));
        break;
      }
    }
    return List.copyOf(launchers.values());
  }

  private static Path resolveExecutable(
      List<Path> searchDirectories,
      String executableName,
      String operatingSystemFamily,
      Map<String, String> environmentVariables
  ) {
    for (Path searchDirectory : searchDirectories) {
      for (String candidateName : executableCandidates(executableName, operatingSystemFamily, environmentVariables)) {
        Path candidate = searchDirectory.resolve(candidateName).normalize();
        if (!Files.isRegularFile(candidate)) {
          continue;
        }
        if ("windows".equals(operatingSystemFamily) || Files.isExecutable(candidate)) {
          return candidate.toAbsolutePath().normalize();
        }
      }
    }
    return null;
  }

  private static List<String> executableCandidates(
      String executableName,
      String operatingSystemFamily,
      Map<String, String> environmentVariables
  ) {
    if (!"windows".equals(operatingSystemFamily) || executableName.contains(".")) {
      return List.of(executableName);
    }

    String pathExtensions = normalize(environmentVariables.get("PATHEXT"));
    List<String> extensions = pathExtensions == null
        ? List.of(".COM", ".EXE", ".BAT", ".CMD")
        : List.of(pathExtensions.split(";", -1));
    List<String> candidates = new ArrayList<>();
    candidates.add(executableName);
    for (String extension : extensions) {
      String normalizedExtension = normalize(extension);
      if (normalizedExtension != null) {
        candidates.add(executableName + normalizedExtension.toLowerCase(Locale.ROOT));
        candidates.add(executableName + normalizedExtension.toUpperCase(Locale.ROOT));
      }
    }
    return List.copyOf(candidates);
  }

  private static List<Path> splitPath(String rawPath) {
    List<Path> results = new ArrayList<>();
    for (String entry : rawPath.split(java.util.regex.Pattern.quote(File.pathSeparator), -1)) {
      String normalized = normalize(entry);
      if (normalized == null) {
        continue;
      }
      try {
        results.add(Path.of(normalized).toAbsolutePath().normalize());
      } catch (InvalidPathException ignored) {
        // Ignore invalid PATH entries and continue with other candidates.
      }
    }
    return List.copyOf(results);
  }

  private static String buildLaunchCommand(Path executablePath, Path workspaceRoot, String operatingSystemFamily) {
    return quote(executablePath.toString(), operatingSystemFamily) + " " + quote(workspaceRoot.toString(), operatingSystemFamily);
  }

  private static String quote(String value, String operatingSystemFamily) {
    String normalized = valueOrDefault(normalize(value), "");
    if ("windows".equals(operatingSystemFamily)) {
      return "\"" + normalized.replace("\"", "\\\"") + "\"";
    }
    return "'" + normalized.replace("'", "'\"'\"'") + "'";
  }

  private static String detectOperatingSystemFamily(String operatingSystemName) {
    String normalized = operatingSystemName.toLowerCase(Locale.ROOT);
    if (normalized.contains("win")) {
      return "windows";
    }
    if (normalized.contains("mac") || normalized.contains("darwin")) {
      return "macos";
    }
    if (normalized.contains("nux") || normalized.contains("linux")) {
      return "linux";
    }
    return "other";
  }

  private static Map<String, String> systemProperties() {
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("os.name", System.getProperty("os.name"));
    properties.put("os.version", System.getProperty("os.version"));
    properties.put("os.arch", System.getProperty("os.arch"));
    return properties;
  }

  private static Map<String, String> normalizeMap(Map<String, String> input) {
    if (input == null || input.isEmpty()) {
      return Map.of();
    }
    Map<String, String> result = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : input.entrySet()) {
      String key = normalize(entry.getKey());
      if (key != null) {
        result.put(key, entry.getValue());
      }
    }
    return result;
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      String normalized = normalize(value);
      if (normalized != null) {
        return normalized;
      }
    }
    return null;
  }

  private static boolean equalsIgnoreCase(String left, String right) {
    String normalizedLeft = normalize(left);
    String normalizedRight = normalize(right);
    return normalizedLeft != null && normalizedRight != null && normalizedLeft.equalsIgnoreCase(normalizedRight);
  }

  private static boolean containsIgnoreCase(String text, String expected) {
    String normalizedText = normalize(text);
    String normalizedExpected = normalize(expected);
    return normalizedText != null
        && normalizedExpected != null
        && normalizedText.toLowerCase(Locale.ROOT).contains(normalizedExpected.toLowerCase(Locale.ROOT));
  }

  private static boolean hasText(String value) {
    return normalize(value) != null;
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private static String valueOrDefault(String value, String fallback) {
    String normalized = normalize(value);
    return normalized == null ? fallback : normalized;
  }

  private record IdeCandidate(String id, String displayName, List<String> executableNames) {
  }
}
