package cn.intentforge.session.local.spi;

import cn.intentforge.session.local.registry.InMemorySessionManager;
import cn.intentforge.session.registry.SessionManager;
import cn.intentforge.session.spi.SessionManagerProvider;

/**
 * Default local {@link SessionManagerProvider} backed by {@link InMemorySessionManager}.
 */
public final class InMemorySessionManagerProvider implements SessionManagerProvider {
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
