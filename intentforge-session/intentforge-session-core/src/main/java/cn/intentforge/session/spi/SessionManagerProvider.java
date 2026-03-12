package cn.intentforge.session.spi;

import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.config.RuntimeImplementationDescriptor;
import cn.intentforge.session.registry.SessionManager;
import java.util.Map;

/**
 * Supplies a {@link SessionManager} implementation for runtime composition.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader}.
 * Higher {@link #priority()} values win when multiple providers are present.
 */
public interface SessionManagerProvider {
  /**
   * Describes the provider for runtime catalog assembly and configuration binding.
   *
   * @return implementation descriptor
   */
  default RuntimeImplementationDescriptor descriptor() {
    return new RuntimeImplementationDescriptor(
        getClass().getName(),
        RuntimeCapability.SESSION_MANAGER,
        getClass().getSimpleName(),
        getClass().getName(),
        Map.of());
  }

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
