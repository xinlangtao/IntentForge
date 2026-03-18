package cn.intentforge.channel.wecom.plugin;

import cn.intentforge.channel.ChannelDriver;
import cn.intentforge.channel.spi.ChannelPlugin;
import cn.intentforge.channel.wecom.driver.WeComChannelDriver;
import java.util.Collection;
import java.util.List;

/**
 * Builtin WeCom intelligent-robot connector plugin.
 *
 * @since 1.0.0
 */
public final class WeComChannelPlugin implements ChannelPlugin {
  private final Collection<ChannelDriver> drivers;

  /**
   * Creates the builtin WeCom intelligent-robot connector plugin.
   */
  public WeComChannelPlugin() {
    this.drivers = List.of(new WeComChannelDriver());
  }

  /**
   * Returns the builtin WeCom driver contribution.
   *
   * @return WeCom driver collection
   */
  @Override
  public Collection<ChannelDriver> drivers() {
    return drivers;
  }
}
