package cn.intentforge.tool.core.spi;

import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.config.RuntimeImplementationDescriptor;
import cn.intentforge.tool.core.registry.ToolRegistry;
import java.util.Map;

/**
 * SPI for supplying custom {@link ToolRegistry} implementation.
 */
public interface ToolRegistryProvider {
  /**
   * Describes the provider for runtime catalog assembly and configuration binding.
   *
   * @return implementation descriptor
   */
  default RuntimeImplementationDescriptor descriptor() {
    return new RuntimeImplementationDescriptor(
        getClass().getName(),
        RuntimeCapability.TOOL_REGISTRY,
        getClass().getSimpleName(),
        getClass().getName(),
        Map.of());
  }

  /**
   * Priority used for provider selection. Higher value wins.
   *
   * @return provider priority
   */
  default int priority() {
    return 0;
  }

  /**
   * Creates a tool registry.
   *
   * @param classLoader class loader used for plugin lookup
   * @return created registry
   */
  ToolRegistry create(ClassLoader classLoader);
}
