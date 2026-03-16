package cn.intentforge.channel.telegram;

import cn.intentforge.channel.ChannelDriver;
import cn.intentforge.channel.spi.ChannelPlugin;
import java.util.Collection;
import java.util.List;

/**
 * Builtin Telegram connector plugin backed by the Telegram Bot API.
 *
 * @since 1.0.0
 */
public final class TelegramChannelPlugin implements ChannelPlugin {
  private final Collection<ChannelDriver> drivers;

  /**
   * Creates the builtin Telegram connector plugin.
   */
  public TelegramChannelPlugin() {
    this.drivers = List.of(new TelegramChannelDriver());
  }

  /**
   * Returns the builtin Telegram driver contribution.
   *
   * @return Telegram driver collection
   */
  @Override
  public Collection<ChannelDriver> drivers() {
    return drivers;
  }
}
