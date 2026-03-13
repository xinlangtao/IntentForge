package cn.intentforge.config;

import static cn.intentforge.common.util.ValidationSupport.normalizeOptional;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Describes one runtime implementation that can be selected by configuration.
 *
 * @param id stable implementation identifier used by configuration
 * @param capability runtime capability provided by the implementation
 * @param displayName human-readable implementation name
 * @param version implementation version text exposed to observability and APIs
 * @param implementationClass concrete implementation class name
 * @param metadata optional implementation metadata
 */
public record RuntimeImplementationDescriptor(
    String id,
    RuntimeCapability capability,
    String displayName,
    String version,
    String implementationClass,
    Map<String, String> metadata
) {
  private static final String INTENTFORGE_PACKAGE_PREFIX = "cn.intentforge.";
  private static final String CURRENT_RUNTIME_VERSION = "nightly-SNAPSHOT";
  private static final String UNKNOWN_VERSION = "unknown";

  /**
   * Creates a validated immutable implementation descriptor.
   */
  public RuntimeImplementationDescriptor {
    id = requireText(id, "id");
    capability = Objects.requireNonNull(capability, "capability must not be null");
    displayName = requireText(displayName, "displayName");
    version = requireText(version, "version");
    implementationClass = requireText(implementationClass, "implementationClass");
    metadata = normalize(metadata);
  }

  /**
   * Creates a descriptor when version metadata is unavailable at the call site.
   *
   * @param id stable implementation identifier used by configuration
   * @param capability runtime capability provided by the implementation
   * @param displayName human-readable implementation name
   * @param implementationClass concrete implementation class name
   * @param metadata optional implementation metadata
   */
  public RuntimeImplementationDescriptor(
      String id,
      RuntimeCapability capability,
      String displayName,
      String implementationClass,
      Map<String, String> metadata
  ) {
    this(id, capability, displayName, UNKNOWN_VERSION, implementationClass, metadata);
  }

  /**
   * Detects one implementation version from packaged metadata with a builtin fallback.
   *
   * @param implementationType implementation type to inspect
   * @return detected version text, builtin runtime version, or {@code unknown}
   */
  public static String detectVersion(Class<?> implementationType) {
    Class<?> nonNullType = Objects.requireNonNull(implementationType, "implementationType must not be null");
    Package implementationPackage = nonNullType.getPackage();
    String implementationVersion = normalizeOptional(implementationPackage == null ? null : implementationPackage.getImplementationVersion());
    if (implementationVersion != null) {
      return implementationVersion;
    }
    String specificationVersion = normalizeOptional(implementationPackage == null ? null : implementationPackage.getSpecificationVersion());
    if (specificationVersion != null) {
      return specificationVersion;
    }
    if (nonNullType.getName().startsWith(INTENTFORGE_PACKAGE_PREFIX)) {
      return CURRENT_RUNTIME_VERSION;
    }
    return UNKNOWN_VERSION;
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
}
