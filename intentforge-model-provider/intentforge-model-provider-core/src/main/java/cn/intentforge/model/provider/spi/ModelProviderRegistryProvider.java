package cn.intentforge.model.provider.spi;

import cn.intentforge.model.provider.registry.ModelProviderRegistry;

/**
 * Supplies a {@link ModelProviderRegistry} implementation for runtime composition.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader}.
 * Higher {@link #priority()} values win when multiple providers are present.
 */
public interface ModelProviderRegistryProvider {
  /**
   * Provider priority for conflict resolution.
   *
   * <p>Higher values are preferred.
   *
   * @return provider priority
   */
  default int priority() {
    return 0;
  }

  /**
   * Creates a model provider registry instance.
   *
   * @param classLoader class loader used by the runtime
   * @return model provider registry instance
   */
  ModelProviderRegistry create(ClassLoader classLoader);
}
