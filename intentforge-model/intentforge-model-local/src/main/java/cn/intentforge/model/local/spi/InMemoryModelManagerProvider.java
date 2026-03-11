package cn.intentforge.model.local.spi;

import cn.intentforge.model.local.registry.InMemoryModelManager;
import cn.intentforge.model.registry.ModelManager;
import cn.intentforge.model.spi.ModelManagerProvider;

/**
 * Default local {@link ModelManagerProvider} backed by {@link InMemoryModelManager}.
 */
public final class InMemoryModelManagerProvider implements ModelManagerProvider {
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
   * Creates the in-memory model manager.
   *
   * @param classLoader class loader used by the runtime
   * @return in-memory model manager
   */
  @Override
  public ModelManager create(ClassLoader classLoader) {
    return new InMemoryModelManager(classLoader);
  }
}
