package cn.intentforge.tool.core.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime context shared by tool invocations.
 */
public final class ToolExecutionContext {
  private final Path workspaceRoot;
  private final Path toolOutputDirectory;
  private final ToolRuntimeEnvironment runtimeEnvironment;
  private final ConcurrentHashMap<String, Object> attributes;

  private ToolExecutionContext(
      Path workspaceRoot,
      Path toolOutputDirectory,
      ToolRuntimeEnvironment runtimeEnvironment,
      Map<String, Object> initialAttributes
  ) {
    this.workspaceRoot = workspaceRoot;
    this.toolOutputDirectory = toolOutputDirectory;
    this.runtimeEnvironment = Objects.requireNonNull(runtimeEnvironment, "runtimeEnvironment must not be null");
    this.attributes = new ConcurrentHashMap<>();
    if (initialAttributes != null && !initialAttributes.isEmpty()) {
      this.attributes.putAll(initialAttributes);
    }
  }

  /**
   * Creates a default context for one workspace.
   *
   * @param workspaceRoot workspace root path
   * @return execution context
   */
  public static ToolExecutionContext create(Path workspaceRoot) {
    Path normalizedWorkspace = normalizeWorkspace(workspaceRoot);
    Path outputDirectory = normalizedWorkspace.resolve(".intentforge").resolve("tool-output");
    ensureDirectory(outputDirectory);
    return new ToolExecutionContext(
        normalizedWorkspace,
        outputDirectory,
        ToolRuntimeEnvironmentDetector.detect(normalizedWorkspace),
        Map.of());
  }

  /**
   * Creates a context with custom output directory and attributes.
   *
   * @param workspaceRoot workspace root path
   * @param toolOutputDirectory output directory
   * @param initialAttributes initial attributes
   * @return execution context
   */
  public static ToolExecutionContext of(
      Path workspaceRoot,
      Path toolOutputDirectory,
      Map<String, Object> initialAttributes
  ) {
    Path normalizedWorkspace = normalizeWorkspace(workspaceRoot);
    return of(
        normalizedWorkspace,
        toolOutputDirectory,
        ToolRuntimeEnvironmentDetector.detect(normalizedWorkspace),
        initialAttributes);
  }

  /**
   * Creates a context with custom output directory, runtime environment, and attributes.
   *
   * @param workspaceRoot workspace root path
   * @param toolOutputDirectory output directory
   * @param runtimeEnvironment runtime environment details
   * @param initialAttributes initial attributes
   * @return execution context
   */
  public static ToolExecutionContext of(
      Path workspaceRoot,
      Path toolOutputDirectory,
      ToolRuntimeEnvironment runtimeEnvironment,
      Map<String, Object> initialAttributes
  ) {
    Path normalizedWorkspace = normalizeWorkspace(workspaceRoot);
    Path normalizedOutput = normalizePath(toolOutputDirectory);
    ensureDirectory(normalizedOutput);
    return new ToolExecutionContext(
        normalizedWorkspace,
        normalizedOutput,
        runtimeEnvironment == null ? ToolRuntimeEnvironmentDetector.detect(normalizedWorkspace) : runtimeEnvironment,
        initialAttributes);
  }

  /**
   * Returns workspace root path.
   *
   * @return workspace root
   */
  public Path workspaceRoot() {
    return workspaceRoot;
  }

  /**
   * Returns tool output directory.
   *
   * @return output directory
   */
  public Path toolOutputDirectory() {
    return toolOutputDirectory;
  }

  /**
   * Returns normalized runtime environment details.
   *
   * @return runtime environment
   */
  public ToolRuntimeEnvironment runtimeEnvironment() {
    return runtimeEnvironment;
  }

  /**
   * Returns an immutable attribute snapshot.
   *
   * @return attribute snapshot
   */
  public Map<String, Object> attributes() {
    return Map.copyOf(new LinkedHashMap<>(attributes));
  }

  /**
   * Reads one attribute.
   *
   * @param key attribute key
   * @return attribute optional
   */
  public Optional<Object> attribute(String key) {
    Objects.requireNonNull(key, "key must not be null");
    return Optional.ofNullable(attributes.get(key));
  }

  /**
   * Writes one attribute.
   *
   * @param key attribute key
   * @param value attribute value
   */
  public void putAttribute(String key, Object value) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(value, "value must not be null");
    attributes.put(key, value);
  }

  /**
   * Resolves one path within workspace.
   *
   * @param path raw path
   * @return normalized workspace path
   */
  public Path resolveWorkspacePath(String path) {
    if (path == null || path.trim().isEmpty()) {
      throw new IllegalArgumentException("path must not be blank");
    }
    return resolveWorkspacePath(Path.of(path.trim()));
  }

  /**
   * Resolves one path within workspace.
   *
   * @param path raw path
   * @return normalized workspace path
   */
  public Path resolveWorkspacePath(Path path) {
    Objects.requireNonNull(path, "path must not be null");
    Path normalized = path.isAbsolute() ? normalizePath(path) : normalizePath(workspaceRoot.resolve(path));
    return ensureWithinWorkspace(normalized);
  }

  /**
   * Ensures one path is under workspace root.
   *
   * @param path path to verify
   * @return normalized path
   */
  public Path ensureWithinWorkspace(Path path) {
    Path normalized = normalizePath(path);
    if (!normalized.startsWith(workspaceRoot)) {
      throw new IllegalArgumentException("Path escapes workspace: " + path);
    }
    return normalized;
  }

  private static Path normalizeWorkspace(Path workspaceRoot) {
    Path normalized = normalizePath(Objects.requireNonNull(workspaceRoot, "workspaceRoot must not be null"));
    ensureDirectory(normalized);
    return normalized;
  }

  private static Path normalizePath(Path path) {
    return path.toAbsolutePath().normalize();
  }

  private static void ensureDirectory(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to create directory: " + path, ex);
    }
  }
}
