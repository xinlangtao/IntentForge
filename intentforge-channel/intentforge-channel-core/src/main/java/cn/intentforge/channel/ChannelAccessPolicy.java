package cn.intentforge.channel;

/**
 * Evaluates whether one inbound channel message is allowed to proceed.
 *
 * @since 1.0.0
 */
public interface ChannelAccessPolicy {
  /**
   * Evaluates one inbound message.
   *
   * @param message inbound message
   * @return access decision
   */
  ChannelAccessDecision evaluate(ChannelInboundMessage message);
}
