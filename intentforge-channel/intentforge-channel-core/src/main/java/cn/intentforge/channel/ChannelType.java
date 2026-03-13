package cn.intentforge.channel;

/**
 * Enumerates supported channel transport families.
 *
 * @since 1.0.0
 */
public enum ChannelType {
  /**
   * Local loopback connector used for development and tests.
   */
  LOOPBACK,
  /**
   * Telegram bot channel.
   */
  TELEGRAM,
  /**
   * WeCom application channel.
   */
  WECOM,
  /**
   * Feishu or Lark channel.
   */
  FEISHU,
  /**
   * Slack application channel.
   */
  SLACK,
  /**
   * Discord bot channel.
   */
  DISCORD,
  /**
   * Vendor specific or user-defined channel.
   */
  CUSTOM
}
