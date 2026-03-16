package cn.intentforge.channel;

import java.util.Objects;
import java.util.Optional;

/**
 * Defines one pluggable channel driver.
 *
 * @since 1.0.0
 */
public interface ChannelDriver {
  /**
   * Returns the descriptor exposed by this driver.
   *
   * @return channel descriptor
   */
  ChannelDescriptor descriptor();

  /**
   * Returns whether this driver supports the provided account profile.
   *
   * @param accountProfile account profile
   * @return {@code true} when supported
   */
  default boolean supports(ChannelAccountProfile accountProfile) {
    ChannelAccountProfile nonNullAccountProfile = Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    return descriptor().type() == nonNullAccountProfile.type();
  }

  /**
   * Opens one account-bound session.
   *
   * @param accountProfile account profile
   * @return opened session
   */
  ChannelSession openSession(ChannelAccountProfile accountProfile);

  /**
   * Opens one webhook handler for the provided account profile when inbound callbacks are supported.
   *
   * @param accountProfile account profile
   * @return opened webhook handler when supported
   */
  default Optional<ChannelWebhookHandler> openWebhookHandler(ChannelAccountProfile accountProfile) {
    Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    return Optional.empty();
  }
}
