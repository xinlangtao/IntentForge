package cn.intentforge.space;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Describes the resources and configuration bound to a single space.
 *
 * @param skillIds configured skill identifiers; {@code null} means inherit from parent
 * @param agentIds configured agent identifiers; {@code null} means inherit from parent
 * @param promptIds configured prompt identifiers; {@code null} means inherit from parent
 * @param toolIds configured tool identifiers; {@code null} means inherit from parent
 * @param modelIds configured model identifiers; {@code null} means inherit from parent
 * @param modelProviderIds configured model provider identifiers; {@code null} means inherit from parent
 * @param memoryIds configured memory identifiers; {@code null} means inherit from parent
 * @param config layered key-value configuration; keys not present inherit from parent spaces
 */
public record SpaceProfile(
    List<String> skillIds,
    List<String> agentIds,
    List<String> promptIds,
    List<String> toolIds,
    List<String> modelIds,
    List<String> modelProviderIds,
    List<String> memoryIds,
    Map<String, String> config
) {
  /**
   * Creates an empty profile that inherits every configurable field.
   *
   * @return empty inheritable profile
   */
  public static SpaceProfile empty() {
    return new SpaceProfile(null, null, null, null, null, null, null, null);
  }

  /**
   * Creates a validated immutable profile.
   *
   * @param skillIds configured skill identifiers
   * @param agentIds configured agent identifiers
   * @param promptIds configured prompt identifiers
   * @param toolIds configured tool identifiers
   * @param modelIds configured model identifiers
   * @param modelProviderIds configured model provider identifiers
   * @param memoryIds configured memory identifiers
   * @param config layered key-value configuration
   */
  public SpaceProfile {
    skillIds = normalizeList(skillIds, "skillIds");
    agentIds = normalizeList(agentIds, "agentIds");
    promptIds = normalizeList(promptIds, "promptIds");
    toolIds = normalizeList(toolIds, "toolIds");
    modelIds = normalizeList(modelIds, "modelIds");
    modelProviderIds = normalizeList(modelProviderIds, "modelProviderIds");
    memoryIds = normalizeList(memoryIds, "memoryIds");
    config = normalizeMap(config, "config");
  }

  private static List<String> normalizeList(List<String> values, String fieldName) {
    if (values == null) {
      return null;
    }
    List<String> normalized = values.stream()
        .map(value -> normalizeScalar(value, fieldName + " item"))
        .toList();
    return List.copyOf(normalized);
  }

  private static Map<String, String> normalizeMap(Map<String, String> values, String fieldName) {
    if (values == null) {
      return null;
    }
    Map<String, String> normalized = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : values.entrySet()) {
      Objects.requireNonNull(entry, fieldName + " entry must not be null");
      String key = normalizeScalar(entry.getKey(), fieldName + " key");
      String value = normalizeScalar(entry.getValue(), fieldName + " value");
      normalized.put(key, value);
    }
    return Map.copyOf(normalized);
  }

  private static String normalizeScalar(String value, String fieldName) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }
}
