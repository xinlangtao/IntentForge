package cn.intentforge.space;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the fully resolved configuration for a target space.
 *
 * @param spaceId resolved target space identifier
 * @param spaceType resolved target space type
 * @param inheritancePath inheritance chain ordered from company to target space
 * @param skillIds effective skill identifiers
 * @param agentIds effective agent identifiers
 * @param promptIds effective prompt identifiers
 * @param toolIds effective tool identifiers
 * @param modelIds effective model identifiers
 * @param modelProviderIds effective model provider identifiers
 * @param memoryIds effective memory identifiers
 * @param config effective layered configuration
 */
public record ResolvedSpaceProfile(
    String spaceId,
    SpaceType spaceType,
    List<String> inheritancePath,
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
   * Creates a validated immutable resolved profile.
   *
   * @param spaceId resolved target space identifier
   * @param spaceType resolved target space type
   * @param inheritancePath inheritance chain ordered from company to target space
   * @param skillIds effective skill identifiers
   * @param agentIds effective agent identifiers
   * @param promptIds effective prompt identifiers
   * @param toolIds effective tool identifiers
   * @param modelIds effective model identifiers
   * @param modelProviderIds effective model provider identifiers
   * @param memoryIds effective memory identifiers
   * @param config effective layered configuration
   */
  public ResolvedSpaceProfile {
    spaceId = requireText(spaceId, "spaceId");
    spaceType = Objects.requireNonNull(spaceType, "spaceType must not be null");
    inheritancePath = normalizeRequiredList(inheritancePath, "inheritancePath");
    skillIds = normalizeRequiredList(skillIds, "skillIds");
    agentIds = normalizeRequiredList(agentIds, "agentIds");
    promptIds = normalizeRequiredList(promptIds, "promptIds");
    toolIds = normalizeRequiredList(toolIds, "toolIds");
    modelIds = normalizeRequiredList(modelIds, "modelIds");
    modelProviderIds = normalizeRequiredList(modelProviderIds, "modelProviderIds");
    memoryIds = normalizeRequiredList(memoryIds, "memoryIds");
    config = normalizeRequiredMap(config, "config");
  }

  private static List<String> normalizeRequiredList(List<String> values, String fieldName) {
    List<String> normalized = Objects.requireNonNull(values, fieldName + " must not be null").stream()
        .map(value -> requireText(value, fieldName + " item"))
        .toList();
    return List.copyOf(normalized);
  }

  private static Map<String, String> normalizeRequiredMap(Map<String, String> values, String fieldName) {
    Map<String, String> source = Objects.requireNonNull(values, fieldName + " must not be null");
    Map<String, String> normalized = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : source.entrySet()) {
      Objects.requireNonNull(entry, fieldName + " entry must not be null");
      String key = requireText(entry.getKey(), fieldName + " key");
      String value = requireText(entry.getValue(), fieldName + " value");
      normalized.put(key, value);
    }
    return Map.copyOf(normalized);
  }

  private static String requireText(String value, String fieldName) {
    String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }
}
