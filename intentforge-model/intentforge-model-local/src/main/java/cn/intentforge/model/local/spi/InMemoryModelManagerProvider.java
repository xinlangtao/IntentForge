package cn.intentforge.model.local.spi;

import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.config.RuntimeImplementationDescriptor;
import cn.intentforge.model.local.registry.InMemoryModelManager;
import cn.intentforge.model.registry.ModelManager;
import cn.intentforge.model.spi.ModelManagerProvider;
import java.util.Map;

/**
 * Default local {@link ModelManagerProvider} backed by {@link InMemoryModelManager}.
 */
public final class InMemoryModelManagerProvider implements ModelManagerProvider {
  /**
   * Returns the stable descriptor for the builtin in-memory model manager.
   *
   * @return builtin descriptor
   */
  @Override
  public RuntimeImplementationDescriptor descriptor() {
    return new RuntimeImplementationDescriptor(
        "intentforge.model.manager.in-memory",
        RuntimeCapability.MODEL_MANAGER,
        "In-Memory Model Manager",
        RuntimeImplementationDescriptor.detectVersion(InMemoryModelManager.class),
        InMemoryModelManager.class.getName(),
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
