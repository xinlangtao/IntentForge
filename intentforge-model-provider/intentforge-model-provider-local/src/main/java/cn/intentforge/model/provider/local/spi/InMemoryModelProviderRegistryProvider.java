package cn.intentforge.model.provider.local.spi;

import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.config.RuntimeImplementationDescriptor;
import cn.intentforge.model.provider.local.registry.InMemoryModelProviderRegistry;
import cn.intentforge.model.provider.registry.ModelProviderRegistry;
import cn.intentforge.model.provider.spi.ModelProviderRegistryProvider;
import java.util.Map;

/**
 * Default local {@link ModelProviderRegistryProvider} backed by {@link InMemoryModelProviderRegistry}.
 */
public final class InMemoryModelProviderRegistryProvider implements ModelProviderRegistryProvider {
  /**
   * Returns the stable descriptor for the builtin in-memory model provider registry.
   *
   * @return builtin descriptor
   */
  @Override
  public RuntimeImplementationDescriptor descriptor() {
    return new RuntimeImplementationDescriptor(
        "intentforge.model-provider.registry.in-memory",
        RuntimeCapability.MODEL_PROVIDER_REGISTRY,
        "In-Memory Model Provider Registry",
        RuntimeImplementationDescriptor.detectVersion(InMemoryModelProviderRegistry.class),
        InMemoryModelProviderRegistry.class.getName(),
        Map.of("builtin", "true", "default", "true"));
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
   * Creates the in-memory model provider registry.
   *
   * @param classLoader class loader used by the runtime
   * @return in-memory model provider registry
   */
  @Override
  public ModelProviderRegistry create(ClassLoader classLoader) {
    return new InMemoryModelProviderRegistry(classLoader);
  }
}
