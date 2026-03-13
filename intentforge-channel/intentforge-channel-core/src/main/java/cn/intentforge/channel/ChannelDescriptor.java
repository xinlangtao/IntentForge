package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.immutableStringMap;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Describes one pluggable channel driver.
 *
 * @param id stable driver identifier
 * @param type channel transport type
 * @param displayName human-readable driver name
 * @param capabilities normalized channel capability list
 * @param metadata driver metadata
 * @since 1.0.0
 */
public record ChannelDescriptor(
    String id,
    ChannelType type,
    String displayName,
    List<ChannelCapability> capabilities,
    java.util.Map<String, String> metadata
) {
  /**
   * Creates one validated immutable descriptor.
   */
  public ChannelDescriptor {
    id = requireText(id, "id");
    type = Objects.requireNonNull(type, "type must not be null");
    displayName = requireText(displayName, "displayName");
    capabilities = immutableCapabilities(capabilities);
    metadata = immutableStringMap(metadata, "metadata");
  }

  /**
   * Returns whether the driver exposes the provided capability.
   *
   * @param capability capability to check
   * @return {@code true} when supported
   */
  public boolean supports(ChannelCapability capability) {
    return capabilities.contains(Objects.requireNonNull(capability, "capability must not be null"));
  }

  private static List<ChannelCapability> immutableCapabilities(List<ChannelCapability> capabilities) {
    if (capabilities == null || capabilities.isEmpty()) {
      return List.of();
    }
    List<ChannelCapability> normalized = new ArrayList<>();
    for (ChannelCapability capability : capabilities) {
      ChannelCapability nonNullCapability = Objects.requireNonNull(capability, "capability must not be null");
      if (!normalized.contains(nonNullCapability)) {
        normalized.add(nonNullCapability);
      }
    }
    return List.copyOf(normalized);
  }
}
