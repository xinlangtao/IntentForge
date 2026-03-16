package cn.intentforge.channel.wecom;

import cn.intentforge.channel.ChannelDriver;
import cn.intentforge.channel.spi.ChannelPlugin;
import java.util.Collection;
import java.util.List;

/**
 * Builtin WeCom connector plugin backed by the WeCom application messaging APIs.
 *
 * @since 1.0.0
 */
public final class WeComChannelPlugin implements ChannelPlugin {
  private final Collection<ChannelDriver> drivers;

  /**
   * Creates the builtin WeCom connector plugin.
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
