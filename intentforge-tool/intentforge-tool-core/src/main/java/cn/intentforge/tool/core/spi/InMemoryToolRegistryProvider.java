package cn.intentforge.tool.core.spi;

import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.config.RuntimeImplementationDescriptor;
import cn.intentforge.tool.core.registry.InMemoryToolRegistry;
import cn.intentforge.tool.core.registry.ToolRegistry;
import java.util.Map;

/**
 * Default {@link ToolRegistryProvider} backed by {@link InMemoryToolRegistry}.
 */
public final class InMemoryToolRegistryProvider implements ToolRegistryProvider {
  /**
   * Returns the stable descriptor for the builtin in-memory tool registry.
   *
   * @return builtin descriptor
   */
  @Override
  public RuntimeImplementationDescriptor descriptor() {
    return new RuntimeImplementationDescriptor(
        "intentforge.tool.registry.in-memory",
        RuntimeCapability.TOOL_REGISTRY,
        "In-Memory Tool Registry",
        InMemoryToolRegistry.class.getName(),
        Map.of("builtin", "true"));
  }

  /**
   * Uses the base priority so custom providers can override it with a higher value.
   *
   * @return provider priority
   */
  @Override
  public int priority() {
    return 0;
  }

  /**
   * Creates the in-memory tool registry.
   *
   * @param classLoader class loader used for plugin lookup
   * @return in-memory tool registry
   */
  @Override
  public ToolRegistry create(ClassLoader classLoader) {
    return new InMemoryToolRegistry(classLoader);
  }
}
