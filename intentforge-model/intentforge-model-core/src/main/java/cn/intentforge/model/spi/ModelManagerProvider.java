package cn.intentforge.model.spi;

import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.config.RuntimeImplementationDescriptor;
import cn.intentforge.model.registry.ModelManager;
import java.util.Map;

/**
 * Supplies a {@link ModelManager} implementation for runtime composition.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader}.
 * Higher {@link #priority()} values win when multiple providers are present.
 */
public interface ModelManagerProvider {
  /**
   * Describes the provider for runtime catalog assembly and configuration binding.
   *
   * @return implementation descriptor
   */
  default RuntimeImplementationDescriptor descriptor() {
    return new RuntimeImplementationDescriptor(
        getClass().getName(),
        RuntimeCapability.MODEL_MANAGER,
        getClass().getSimpleName(),
        RuntimeImplementationDescriptor.detectVersion(getClass()),
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
   * Creates a model manager instance.
   *
   * @param classLoader class loader used by the runtime
   * @return model manager instance
   */
  ModelManager create(ClassLoader classLoader);
}
