package cn.intentforge.model.provider;

import static cn.intentforge.common.util.ValidationSupport.normalize;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import cn.intentforge.model.catalog.ModelCapability;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ModelProviderDescriptor(
    String id,
    String displayName,
    String description,
    ModelProviderType type,
    String endpoint,
    List<ModelCapability> capabilities,
    Map<String, String> metadata
) {
  public ModelProviderDescriptor {
    id = requireText(id, "id");
    displayName = normalize(displayName);
    description = normalize(description);
    type = Objects.requireNonNull(type, "type must not be null");
    endpoint = normalize(endpoint);
    capabilities = immutableCapabilities(capabilities);
    metadata = immutableMetadata(metadata);
  }

  private static List<ModelCapability> immutableCapabilities(List<ModelCapability> capabilities) {
    if (capabilities == null || capabilities.isEmpty()) {
      return List.of();
    }
    List<ModelCapability> normalizedCapabilities = new ArrayList<>();
    for (ModelCapability capability : capabilities) {
      ModelCapability nonNullCapability = Objects.requireNonNull(capability, "capability must not be null");
      if (!normalizedCapabilities.contains(nonNullCapability)) {
        normalizedCapabilities.add(nonNullCapability);
      }
    }
    return List.copyOf(normalizedCapabilities);
  }

  private static Map<String, String> immutableMetadata(Map<String, String> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return Map.of();
    }
    Map<String, String> normalizedMetadata = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : metadata.entrySet()) {
      String key = requireText(entry.getKey(), "metadata key");
      String value = Objects.requireNonNull(entry.getValue(), "metadata value must not be null");
      normalizedMetadata.put(key, value);
    }
    return Map.copyOf(normalizedMetadata);
  }
}
