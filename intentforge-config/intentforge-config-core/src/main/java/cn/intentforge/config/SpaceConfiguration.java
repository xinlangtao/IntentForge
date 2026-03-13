package cn.intentforge.config;

import static cn.intentforge.common.util.ValidationSupport.immutableStringMap;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.util.List;
import java.util.Map;

/**
 * User-managed configuration snapshot for one space.
 *
 * <p>This contract belongs to the `config` module because UI and API layers persist and mutate it as configuration data.
 * `intentforge-space` consumes these values as runtime-facing space bindings.
 *
 * @param spaceId target space identifier
 * @param skillIds configured skill identifiers
 * @param agentIds configured agent identifiers
 * @param promptIds configured prompt identifiers
 * @param toolIds configured tool identifiers
 * @param modelIds configured model identifiers
 * @param modelProviderIds configured model-provider identifiers
 * @param memoryIds configured memory identifiers
 * @param properties layered string properties
 * @param runtimeBindings configured runtime implementation bindings
 */
public record SpaceConfiguration(
    String spaceId,
    List<String> skillIds,
    List<String> agentIds,
    List<String> promptIds,
    List<String> toolIds,
    List<String> modelIds,
    List<String> modelProviderIds,
    List<String> memoryIds,
    Map<String, String> properties,
    RuntimeBindings runtimeBindings
) {
  /**
   * Creates a validated immutable space configuration snapshot.
   */
  public SpaceConfiguration {
    spaceId = requireText(spaceId, "spaceId");
    skillIds = immutableTextList(skillIds, "skillIds");
    agentIds = immutableTextList(agentIds, "agentIds");
    promptIds = immutableTextList(promptIds, "promptIds");
    toolIds = immutableTextList(toolIds, "toolIds");
    modelIds = immutableTextList(modelIds, "modelIds");
    modelProviderIds = immutableTextList(modelProviderIds, "modelProviderIds");
    memoryIds = immutableTextList(memoryIds, "memoryIds");
    properties = immutableStringMap(properties, "properties");
    runtimeBindings = runtimeBindings == null ? RuntimeBindings.empty() : runtimeBindings;
  }

  private static List<String> immutableTextList(List<String> values, String fieldName) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    List<String> normalized = values.stream().map(value -> requireText(value, fieldName + " item")).toList();
    return List.copyOf(normalized);
  }
}
