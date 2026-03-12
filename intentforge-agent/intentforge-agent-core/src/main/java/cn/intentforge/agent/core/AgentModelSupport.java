package cn.intentforge.agent.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class AgentModelSupport {
  private AgentModelSupport() {
  }

  static String requireText(String value, String fieldName) {
    String normalized = normalize(value);
    if (normalized == null) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }

  static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  static Map<String, String> immutableMetadata(Map<String, String> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return Map.of();
    }
    Map<String, String> normalized = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : metadata.entrySet()) {
      Objects.requireNonNull(entry, "metadata entry must not be null");
      normalized.put(
          requireText(entry.getKey(), "metadata key"),
          requireText(entry.getValue(), "metadata value"));
    }
    return Map.copyOf(normalized);
  }

  static Map<String, Object> immutableObjectMap(Map<String, Object> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> normalized = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : metadata.entrySet()) {
      Objects.requireNonNull(entry, "metadata entry must not be null");
      normalized.put(
          requireText(entry.getKey(), "metadata key"),
          Objects.requireNonNull(entry.getValue(), "metadata value must not be null"));
    }
    return Map.copyOf(normalized);
  }

  static <T> List<T> immutableList(List<T> values, String fieldName) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    List<T> normalized = new ArrayList<>(values.size());
    for (T value : values) {
      normalized.add(Objects.requireNonNull(value, fieldName + " item must not be null"));
    }
    return List.copyOf(normalized);
  }

  static Path normalizeWorkspace(Path workspaceRoot) {
    Path normalized = Objects.requireNonNull(workspaceRoot, "workspaceRoot must not be null").toAbsolutePath().normalize();
    try {
      Files.createDirectories(normalized);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to create workspace directory: " + normalized, ex);
    }
    return normalized;
  }
}
