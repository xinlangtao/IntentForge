package cn.intentforge.agent.core;

import cn.intentforge.common.util.ValidationSupport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class AgentModelSupport {
  private AgentModelSupport() {
  }

  static Map<String, String> immutableMetadata(Map<String, String> metadata) {
    return ValidationSupport.immutableStringMap(metadata, "metadata");
  }

  static Map<String, Object> immutableObjectMap(Map<String, Object> metadata) {
    return ValidationSupport.immutableObjectMap(metadata, "metadata");
  }

  static <T> List<T> immutableList(List<T> values, String fieldName) {
    return ValidationSupport.immutableList(values, fieldName);
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
