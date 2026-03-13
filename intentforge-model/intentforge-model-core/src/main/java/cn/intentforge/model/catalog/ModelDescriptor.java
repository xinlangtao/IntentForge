package cn.intentforge.model.catalog;

import static cn.intentforge.common.util.ValidationSupport.normalize;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ModelDescriptor(
    String id,
    String providerId,
    String displayName,
    String description,
    ModelType type,
    List<ModelCapability> capabilities,
    int contextWindow,
    boolean streaming,
    Map<String, String> metadata
) {
  public ModelDescriptor {
    id = requireText(id, "id");
    providerId = normalize(providerId);
    displayName = normalize(displayName);
    description = normalize(description);
    type = Objects.requireNonNull(type, "type must not be null");
    capabilities = immutableCapabilities(capabilities);
    if (contextWindow < 0) {
      throw new IllegalArgumentException("contextWindow must not be negative");
    }
    metadata = immutableMetadata(metadata);
  }

  public boolean matches(ModelQuery query) {
    if (query == null) {
      return true;
    }
    if (query.providerId() != null && !query.providerId().equals(providerId)) {
      return false;
    }
    if (query.type() != null && query.type() != type) {
      return false;
    }
    if (query.capability() != null && !capabilities.contains(query.capability())) {
      return false;
    }
    return query.streaming() == null || query.streaming() == streaming;
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
