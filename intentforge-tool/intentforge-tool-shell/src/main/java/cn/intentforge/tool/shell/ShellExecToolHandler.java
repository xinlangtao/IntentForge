package cn.intentforge.tool.shell;

import cn.intentforge.tool.core.ToolHandler;
import cn.intentforge.tool.core.model.ToolCallRequest;
import cn.intentforge.tool.core.model.ToolCallResult;
import cn.intentforge.tool.core.model.ToolExecutionContext;
import cn.intentforge.tool.core.model.ToolRuntimeEnvironment;
import cn.intentforge.tool.core.util.OutputTruncator;
import cn.intentforge.tool.shell.security.ShellCommandValidator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Handler for {@code intentforge.shell.exec}.
 */
public final class ShellExecToolHandler implements ToolHandler {
  private final Set<String> allowedExecutables;
  private final ShellCommandValidator commandValidator;
  private final int defaultTimeoutMs;
  private final int maxInlineChars;

  /**
   * Creates handler with default options.
   */
  public ShellExecToolHandler() {
    this(Set.of(), new cn.intentforge.tool.shell.security.DefaultShellCommandValidator(), 120_000, 12_000);
  }

  /**
   * Creates handler.
   *
   * @param allowedExecutables executable whitelist
   * @param commandValidator command validator
   * @param defaultTimeoutMs default timeout in milliseconds
   * @param maxInlineChars max inline output characters
   */
  public ShellExecToolHandler(
      Set<String> allowedExecutables,
      ShellCommandValidator commandValidator,
      int defaultTimeoutMs,
      int maxInlineChars
  ) {
    this.allowedExecutables = normalizeAllowed(allowedExecutables);
    this.commandValidator = Objects.requireNonNull(commandValidator, "commandValidator must not be null");
    this.defaultTimeoutMs = Math.max(1, defaultTimeoutMs);
    this.maxInlineChars = Math.max(1, maxInlineChars);
  }

  @Override
  public ToolCallResult handle(ToolCallRequest request) {
    String command = readString(request.parameters(), "command");
    if (command == null) {
      return ToolCallResult.error("SHELL_INVALID_ARGUMENT", "command is required");
    }

    ToolExecutionContext context = request.context();
    String workingDirectoryText = readString(request.parameters(), "workdir");
    Path workingDirectory = workingDirectoryText == null
        ? context.workspaceRoot()
        : context.resolveWorkspacePath(workingDirectoryText);

    int timeoutMs = readInt(request.parameters(), "timeoutMs", defaultTimeoutMs);
    boolean loginShell = readBoolean(request.parameters(), "loginShell", true);
    boolean tty = readBoolean(request.parameters(), "tty", false);

    ShellCommandValidator.ValidationResult validation =
        commandValidator.validate(command, workingDirectory, allowedExecutables);
    if (!validation.allowed()) {
      return ToolCallResult.error("SHELL_COMMAND_REJECTED", validation.reason());
    }

    try {
      ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(command, loginShell, context.runtimeEnvironment()));
      processBuilder.directory(workingDirectory.toFile());
      Process process = processBuilder.start();

      String stdout;
      String stderr;
      boolean timedOut;
      int exitCode;
      try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
        Future<String> stdoutFuture = executor.submit(() -> readAll(process.getInputStream()));
        Future<String> stderrFuture = executor.submit(() -> readAll(process.getErrorStream()));

        timedOut = !process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        if (timedOut) {
          process.destroyForcibly();
          process.waitFor(5, TimeUnit.SECONDS);
        }

        exitCode = process.exitValue();
        stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
        stderr = stderrFuture.get(5, TimeUnit.SECONDS);
      }

      OutputTruncator.TruncateResult stdoutResult =
          OutputTruncator.truncate(stdout, maxInlineChars, context, "shell-stdout");
      OutputTruncator.TruncateResult stderrResult =
          OutputTruncator.truncate(stderr, maxInlineChars, context, "shell-stderr");

      Map<String, Object> structured = new LinkedHashMap<>();
      structured.put("exitCode", exitCode);
      structured.put("stdout", stdoutResult.content());
      structured.put("stderr", stderrResult.content());
      structured.put("timedOut", timedOut);
      structured.put("aborted", false);

      Map<String, Object> metadata = new LinkedHashMap<>();
      metadata.put("workdir", workingDirectory.toString());
      metadata.put("command", command);
      metadata.put("tty", tty);
      metadata.put("loginShell", loginShell);
      metadata.put("stdoutTruncated", stdoutResult.truncated());
      metadata.put("stderrTruncated", stderrResult.truncated());
      if (stdoutResult.outputPath() != null) {
        metadata.put("stdoutOutputPath", stdoutResult.outputPath().toString());
      }
      if (stderrResult.outputPath() != null) {
        metadata.put("stderrOutputPath", stderrResult.outputPath().toString());
      }

      String output = "exitCode=" + exitCode + "\nstdout:\n" + stdoutResult.content() + "\nstderr:\n" + stderrResult.content();
      return ToolCallResult.success(output, Map.copyOf(structured), Map.copyOf(metadata));
    } catch (Exception ex) {
      return ToolCallResult.error("SHELL_EXECUTION_ERROR", ex.getMessage());
    }
  }

  private static List<String> buildCommand(
      String command,
      boolean loginShell,
      ToolRuntimeEnvironment runtimeEnvironment
  ) {
    if ("windows".equals(runtimeEnvironment.operatingSystemFamily())) {
      return List.of(runtimeEnvironment.shell().executable(), "/c", command);
    }

    List<String> args = new ArrayList<>();
    args.add(runtimeEnvironment.shell().executable());
    args.add(loginShell ? "-lc" : "-c");
    args.add(command);
    return List.copyOf(args);
  }

  private static String readAll(InputStream inputStream) {
    try {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to read process stream", ex);
    }
  }

  private static int readInt(Map<String, Object> parameters, String key, int defaultValue) {
    Object value = parameters.get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Number number) {
      return Math.max(1, number.intValue());
    }
    return Math.max(1, Integer.parseInt(String.valueOf(value)));
  }

  private static String readString(Map<String, Object> parameters, String key) {
    Object value = parameters.get(key);
    if (value == null) {
      return null;
    }
    String normalized = String.valueOf(value).trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private static boolean readBoolean(Map<String, Object> parameters, String key, boolean defaultValue) {
    Object value = parameters.get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Boolean bool) {
      return bool;
    }
    return Boolean.parseBoolean(String.valueOf(value));
  }

  private static Set<String> normalizeAllowed(Set<String> allowedExecutables) {
    if (allowedExecutables == null || allowedExecutables.isEmpty()) {
      return Set.of();
    }
    return allowedExecutables.stream()
        .map(value -> value == null ? null : value.trim().toLowerCase(Locale.ROOT))
        .filter(value -> value != null && !value.isEmpty())
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }
}
