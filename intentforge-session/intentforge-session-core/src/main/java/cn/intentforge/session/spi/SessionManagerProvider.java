package cn.intentforge.session.spi;

import cn.intentforge.session.registry.SessionManager;

/**
 * Supplies a {@link SessionManager} implementation for runtime composition.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader}.
 * Higher {@link #priority()} values win when multiple providers are present.
 */
public interface SessionManagerProvider {
  /**
   * Provider priority used for conflict resolution.
   *
   * @return provider priority
   */
  default int priority() {
    return 0;
  }

  /**
   * Creates a session manager instance.
   *
   * @param classLoader class loader used by the runtime
   * @return session manager instance
   */
  SessionManager create(ClassLoader classLoader);
}
