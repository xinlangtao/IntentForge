package cn.intentforge.tool.validation;

import cn.intentforge.tool.core.ToolHandler;
import cn.intentforge.tool.core.model.ToolCallRequest;
import cn.intentforge.tool.core.model.ToolCallResult;
import cn.intentforge.tool.core.model.ToolRuntimeEnvironment;
import cn.intentforge.tool.validation.model.ValidationReport;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Command-based validation handler used by build/test/lint tools.
 */
final class ValidationCommandToolHandler implements ToolHandler {
  private static final int MAX_LOG_EXCERPT_CHARS = 8_000;

  private final String checkName;
  private final String defaultCommand;
  private final int defaultTimeoutMs;

  ValidationCommandToolHandler(String checkName, String defaultCommand, int defaultTimeoutMs) {
    this.checkName = normalize(checkName) == null ? "validation" : checkName;
    this.defaultCommand = normalize(defaultCommand);
    this.defaultTimeoutMs = Math.max(1, defaultTimeoutMs);
  }

  @Override
  public ToolCallResult handle(ToolCallRequest request) {
    String command = readString(request.parameters(), "command");
    if (command == null) {
      command = defaultCommand;
    }
    if (command == null) {
      return ToolCallResult.error("VALIDATION_INVALID_ARGUMENT", "command is required");
    }

    Path workdir = readString(request.parameters(), "workdir") == null
        ? request.context().workspaceRoot()
        : request.context().resolveWorkspacePath(readString(request.parameters(), "workdir"));
    int timeoutMs = readInt(request.parameters(), "timeoutMs", defaultTimeoutMs);
    Instant startedAt = Instant.now();

    try {
      ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(command, request.context().runtimeEnvironment()));
      processBuilder.directory(workdir.toFile());
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
          process.waitFor(2, TimeUnit.SECONDS);
        }
        exitCode = timedOut ? 124 : process.exitValue();
        stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
        stderr = stderrFuture.get(5, TimeUnit.SECONDS);
      }

      long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
      String combinedLog = (stdout == null ? "" : stdout) + (stderr == null || stderr.isEmpty() ? "" : "\n" + stderr);
      String excerpt = excerpt(combinedLog);

      boolean success = !timedOut && exitCode == 0;
      List<String> failedChecks = success ? List.of() : List.of(buildFailedMessage(exitCode, timedOut));
      ValidationReport report = new ValidationReport(success, failedChecks, excerpt, durationMs);

      Map<String, Object> metadata = new LinkedHashMap<>();
      metadata.put("check", checkName);
      metadata.put("command", command);
      metadata.put("workdir", workdir.toString());
      metadata.put("exitCode", exitCode);
      metadata.put("timedOut", timedOut);
      metadata.put("durationMs", durationMs);

      String output = success
          ? checkName + " passed in " + durationMs + "ms"
          : checkName + " failed in " + durationMs + "ms: " + failedChecks.getFirst();
      return ToolCallResult.success(output, report.toMap(), Map.copyOf(metadata));
    } catch (Exception ex) {
      return ToolCallResult.error("VALIDATION_EXECUTION_ERROR", ex.getMessage());
    }
  }

  private String buildFailedMessage(int exitCode, boolean timedOut) {
    if (timedOut) {
      return checkName + " timed out";
    }
    return checkName + " exited with code " + exitCode;
  }

  private static String excerpt(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    if (value.length() <= MAX_LOG_EXCERPT_CHARS) {
      return value;
    }
    return value.substring(0, MAX_LOG_EXCERPT_CHARS) + "\n... excerpt truncated ...";
  }

  private static String readAll(InputStream inputStream) {
    try {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to read process stream", ex);
    }
  }

  private static List<String> buildCommand(String command, ToolRuntimeEnvironment runtimeEnvironment) {
    if ("windows".equals(runtimeEnvironment.operatingSystemFamily())) {
      return List.of(runtimeEnvironment.shell().executable(), "/c", command);
    }
    List<String> args = new ArrayList<>();
    args.add(runtimeEnvironment.shell().executable());
    args.add(runtimeEnvironment.shell().loginShellPreferred() ? "-lc" : "-c");
    args.add(command);
    return List.copyOf(args);
  }

  private static String readString(Map<String, Object> parameters, String key) {
    return normalize(parameters.get(key));
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

  private static String normalize(Object value) {
    if (value == null) {
      return null;
    }
    String normalized = String.valueOf(value).trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
