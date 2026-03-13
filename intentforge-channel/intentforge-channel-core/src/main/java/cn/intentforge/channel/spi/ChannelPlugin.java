package cn.intentforge.channel.spi;

import cn.intentforge.channel.ChannelDriver;
import java.util.Collection;

/**
 * Provides channel driver contributions.
 *
 * @since 1.0.0
 */
public interface ChannelPlugin {
  /**
   * Returns all drivers contributed by this plugin.
   *
   * @return driver contributions
   */
  Collection<ChannelDriver> drivers();
}
