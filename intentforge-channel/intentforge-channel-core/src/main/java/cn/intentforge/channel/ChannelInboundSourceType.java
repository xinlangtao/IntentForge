package cn.intentforge.channel;

/**
 * Enumerates the transport-level ingress styles that may deliver normalized inbound channel messages.
 *
 * @since 1.0.0
 */
public enum ChannelInboundSourceType {
  /**
   * Inbound messages delivered by an external webhook callback.
   */
  WEBHOOK,
  /**
   * Inbound messages delivered by a connector-owned long-polling loop.
   */
  LONG_POLLING
}
