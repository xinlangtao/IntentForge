package cn.intentforge.channel;

/**
 * Represents one account-bound runtime session opened by a channel driver.
 *
 * @since 1.0.0
 */
public interface ChannelSession {
  /**
   * Returns the bound account profile.
   *
   * @return account profile
   */
  ChannelAccountProfile accountProfile();

  /**
   * Sends one outbound request.
   *
   * @param request outbound request
   * @return delivery result
   */
  ChannelDeliveryResult send(ChannelOutboundRequest request);

  /**
   * Closes the session.
   */
  default void close() {
  }
}
