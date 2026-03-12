package cn.intentforge.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable catalog of runtime implementations discovered from SPI and plugins.
 *
 * @param implementationsByCapability implementations grouped by capability
 */
public record RuntimeCatalog(
    Map<RuntimeCapability, List<RuntimeImplementationDescriptor>> implementationsByCapability
) {
  /**
   * Creates one empty runtime catalog.
   *
   * @return empty catalog
   */
  public static RuntimeCatalog empty() {
    return new RuntimeCatalog(Map.of());
  }

  /**
   * Creates one runtime catalog from discovered descriptors.
   *
   * @param descriptors discovered implementation descriptors
   * @return grouped catalog
   */
  public static RuntimeCatalog of(List<RuntimeImplementationDescriptor> descriptors) {
    Map<RuntimeCapability, List<RuntimeImplementationDescriptor>> grouped = new EnumMap<>(RuntimeCapability.class);
    for (RuntimeImplementationDescriptor descriptor : Objects.requireNonNull(descriptors, "descriptors must not be null")) {
      RuntimeImplementationDescriptor nonNullDescriptor = Objects.requireNonNull(descriptor, "descriptor must not be null");
      grouped.computeIfAbsent(nonNullDescriptor.capability(), ignored -> new ArrayList<>()).add(nonNullDescriptor);
    }
    Map<RuntimeCapability, List<RuntimeImplementationDescriptor>> normalized = new EnumMap<>(RuntimeCapability.class);
    for (Map.Entry<RuntimeCapability, List<RuntimeImplementationDescriptor>> entry : grouped.entrySet()) {
      List<RuntimeImplementationDescriptor> ordered = entry.getValue().stream()
          .sorted(Comparator.comparing(RuntimeImplementationDescriptor::id))
          .toList();
      normalized.put(entry.getKey(), List.copyOf(ordered));
    }
    return new RuntimeCatalog(normalized);
  }

  /**
   * Creates a validated immutable runtime catalog.
   */
  public RuntimeCatalog {
    Map<RuntimeCapability, List<RuntimeImplementationDescriptor>> source =
        Objects.requireNonNull(implementationsByCapability, "implementationsByCapability must not be null");
    Map<RuntimeCapability, List<RuntimeImplementationDescriptor>> normalized = new EnumMap<>(RuntimeCapability.class);
    for (Map.Entry<RuntimeCapability, List<RuntimeImplementationDescriptor>> entry : source.entrySet()) {
      Objects.requireNonNull(entry, "implementationsByCapability entry must not be null");
      RuntimeCapability capability = Objects.requireNonNull(entry.getKey(), "runtime catalog capability must not be null");
      List<RuntimeImplementationDescriptor> descriptors = Objects.requireNonNull(entry.getValue(), "runtime catalog descriptors must not be null")
          .stream()
          .map(descriptor -> Objects.requireNonNull(descriptor, "runtime catalog descriptor must not be null"))
          .toList();
      normalized.put(capability, List.copyOf(descriptors));
    }
    implementationsByCapability = Map.copyOf(new LinkedHashMap<>(normalized));
  }

  /**
   * Lists implementations for the provided capability.
   *
   * @param capability target capability
   * @return discovered implementations
   */
  public List<RuntimeImplementationDescriptor> list(RuntimeCapability capability) {
    return implementationsByCapability.getOrDefault(
        Objects.requireNonNull(capability, "capability must not be null"),
        List.of());
  }

  /**
   * Finds one implementation by capability and identifier.
   *
   * @param capability target capability
   * @param implementationId implementation identifier
   * @return matching descriptor when present
   */
  public Optional<RuntimeImplementationDescriptor> find(RuntimeCapability capability, String implementationId) {
    Objects.requireNonNull(implementationId, "implementationId must not be null");
    return list(capability).stream()
        .filter(descriptor -> descriptor.id().equals(implementationId))
        .findFirst();
  }
}
