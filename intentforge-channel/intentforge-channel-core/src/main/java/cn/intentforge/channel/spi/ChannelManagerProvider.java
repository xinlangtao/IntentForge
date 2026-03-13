package cn.intentforge.channel.spi;

import cn.intentforge.channel.registry.ChannelManager;
import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.config.RuntimeImplementationDescriptor;
import java.util.Map;

/**
 * Supplies a {@link ChannelManager} implementation for runtime composition.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader}.
 * Higher {@link #priority()} values win when multiple providers are present.
 *
 * @since 1.0.0
 */
public interface ChannelManagerProvider {
  /**
   * Describes the provider for runtime catalog assembly and configuration binding.
   *
   * @return implementation descriptor
   */
  default RuntimeImplementationDescriptor descriptor() {
    return new RuntimeImplementationDescriptor(
        getClass().getName(),
        RuntimeCapability.CHANNEL_MANAGER,
        getClass().getSimpleName(),
        RuntimeImplementationDescriptor.detectVersion(getClass()),
        getClass().getName(),
        Map.of());
  }

  /**
   * Provider priority for conflict resolution.
   *
   * @return provider priority
   */
  default int priority() {
    return 0;
  }

  /**
   * Creates a channel manager instance.
   *
   * @param classLoader class loader used by the runtime
   * @return channel manager instance
   */
  ChannelManager create(ClassLoader classLoader);
}
