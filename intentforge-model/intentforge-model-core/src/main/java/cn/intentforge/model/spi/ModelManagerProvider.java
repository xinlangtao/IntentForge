package cn.intentforge.model.spi;

import cn.intentforge.model.registry.ModelManager;

/**
 * Supplies a {@link ModelManager} implementation for runtime composition.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader}.
 * Higher {@link #priority()} values win when multiple providers are present.
 */
public interface ModelManagerProvider {
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
