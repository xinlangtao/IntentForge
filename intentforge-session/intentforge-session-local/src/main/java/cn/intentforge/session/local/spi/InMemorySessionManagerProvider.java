package cn.intentforge.session.local.spi;

import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.config.RuntimeImplementationDescriptor;
import cn.intentforge.session.local.registry.InMemorySessionManager;
import cn.intentforge.session.registry.SessionManager;
import cn.intentforge.session.spi.SessionManagerProvider;
import java.util.Map;

/**
 * Default local {@link SessionManagerProvider} backed by {@link InMemorySessionManager}.
 */
public final class InMemorySessionManagerProvider implements SessionManagerProvider {
  /**
   * Returns the stable descriptor for the builtin in-memory session manager.
   *
   * @return builtin descriptor
   */
  @Override
  public RuntimeImplementationDescriptor descriptor() {
    return new RuntimeImplementationDescriptor(
        "intentforge.session.manager.in-memory",
        RuntimeCapability.SESSION_MANAGER,
        "In-Memory Session Manager",
        InMemorySessionManager.class.getName(),
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
   * Creates the in-memory session manager.
   *
   * @param classLoader class loader used by the runtime
   * @return in-memory session manager
   */
  @Override
  public SessionManager create(ClassLoader classLoader) {
    return new InMemorySessionManager(classLoader);
  }
}
