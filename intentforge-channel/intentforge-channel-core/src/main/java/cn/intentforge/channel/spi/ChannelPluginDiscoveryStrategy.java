package cn.intentforge.channel.spi;

import java.util.Collection;

/**
 * Discovers additional {@link ChannelPlugin} implementations from integration-specific SPI mechanisms.
 *
 * @since 1.0.0
 */
public interface ChannelPluginDiscoveryStrategy {
  /**
   * Discovers channel plugins for the provided class loader.
   *
   * @param classLoader class loader used for discovery
   * @return discovered plugins
   */
  Collection<ChannelPlugin> load(ClassLoader classLoader);
}
