package cn.intentforge.model.provider.spi;

import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.config.RuntimeImplementationDescriptor;
import cn.intentforge.model.provider.registry.ModelProviderRegistry;
import java.util.Map;

/**
 * Supplies a {@link ModelProviderRegistry} implementation for runtime composition.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader}.
 * Higher {@link #priority()} values win when multiple providers are present.
 */
public interface ModelProviderRegistryProvider {
  /**
   * Describes the provider for runtime catalog assembly and configuration binding.
   *
   * @return implementation descriptor
   */
  default RuntimeImplementationDescriptor descriptor() {
    return new RuntimeImplementationDescriptor(
        getClass().getName(),
        RuntimeCapability.MODEL_PROVIDER_REGISTRY,
        getClass().getSimpleName(),
        getClass().getName(),
        Map.of());
  }

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
