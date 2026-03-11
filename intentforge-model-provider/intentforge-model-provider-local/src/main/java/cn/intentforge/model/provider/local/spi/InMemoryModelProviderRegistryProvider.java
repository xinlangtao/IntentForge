package cn.intentforge.model.provider.local.spi;

import cn.intentforge.model.provider.local.registry.InMemoryModelProviderRegistry;
import cn.intentforge.model.provider.registry.ModelProviderRegistry;
import cn.intentforge.model.provider.spi.ModelProviderRegistryProvider;

/**
 * Default local {@link ModelProviderRegistryProvider} backed by {@link InMemoryModelProviderRegistry}.
 */
public final class InMemoryModelProviderRegistryProvider implements ModelProviderRegistryProvider {
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
