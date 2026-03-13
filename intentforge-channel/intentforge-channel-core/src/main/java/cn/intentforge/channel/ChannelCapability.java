package cn.intentforge.channel;

/**
 * Enumerates normalized channel capabilities exposed by a driver.
 *
 * @since 1.0.0
 */
public enum ChannelCapability {
  /**
   * Driver can send outbound text messages.
   */
  SEND_MESSAGES,
  /**
   * Driver can receive inbound messages.
   */
  RECEIVE_MESSAGES,
  /**
   * Driver supports thread-aware replies.
   */
  THREAD_REPLIES,
  /**
   * Driver supports interactive callbacks or button events.
   */
  INTERACTIVE_CALLBACKS,
  /**
   * Driver supports file or media delivery.
   */
  FILE_TRANSFER
}
