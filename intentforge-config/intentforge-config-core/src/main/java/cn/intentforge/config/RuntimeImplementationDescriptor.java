package cn.intentforge.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Describes one runtime implementation that can be selected by configuration.
 *
 * @param id stable implementation identifier used by configuration
 * @param capability runtime capability provided by the implementation
 * @param displayName human-readable implementation name
 * @param implementationClass concrete implementation class name
 * @param metadata optional implementation metadata
 */
public record RuntimeImplementationDescriptor(
    String id,
    RuntimeCapability capability,
    String displayName,
    String implementationClass,
    Map<String, String> metadata
) {
  /**
   * Creates a validated immutable implementation descriptor.
   */
  public RuntimeImplementationDescriptor {
    id = requireText(id, "id");
    capability = Objects.requireNonNull(capability, "capability must not be null");
    displayName = requireText(displayName, "displayName");
    implementationClass = requireText(implementationClass, "implementationClass");
    metadata = normalize(metadata);
  }

  private static Map<String, String> normalize(Map<String, String> values) {
    if (values == null) {
      return Map.of();
    }
    Map<String, String> normalized = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : values.entrySet()) {
      Objects.requireNonNull(entry, "metadata entry must not be null");
      normalized.put(requireText(entry.getKey(), "metadata key"), requireText(entry.getValue(), "metadata value"));
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
