package cn.intentforge.channel.local.spi;

import cn.intentforge.channel.local.registry.InMemoryChannelManager;
import cn.intentforge.channel.registry.ChannelManager;
import cn.intentforge.channel.spi.ChannelManagerProvider;
import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.config.RuntimeImplementationDescriptor;
import java.util.Map;

/**
 * Default local {@link ChannelManagerProvider} backed by {@link InMemoryChannelManager}.
 *
 * @since 1.0.0
 */
public final class InMemoryChannelManagerProvider implements ChannelManagerProvider {
  /**
   * Returns the stable descriptor for the builtin in-memory channel manager.
   *
   * @return builtin descriptor
   */
  @Override
  public RuntimeImplementationDescriptor descriptor() {
    return new RuntimeImplementationDescriptor(
        "intentforge.channel.manager.in-memory",
        RuntimeCapability.CHANNEL_MANAGER,
        "In-Memory Channel Manager",
        RuntimeImplementationDescriptor.detectVersion(InMemoryChannelManager.class),
        InMemoryChannelManager.class.getName(),
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
   * Creates the in-memory channel manager.
   *
   * @param classLoader class loader used by the runtime
   * @return in-memory channel manager
   */
  @Override
  public ChannelManager create(ClassLoader classLoader) {
    return new InMemoryChannelManager(classLoader);
  }
}
