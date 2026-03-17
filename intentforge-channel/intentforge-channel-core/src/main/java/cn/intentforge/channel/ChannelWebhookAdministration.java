package cn.intentforge.channel;

import java.util.Optional;

/**
 * Defines one account-bound webhook administration contract.
 *
 * @since 1.0.0
 */
public interface ChannelWebhookAdministration {
  /**
   * Registers or updates the webhook endpoint for the bound account.
   *
   * @param registration desired webhook registration
   */
  void setWebhook(ChannelWebhookRegistration registration);

  /**
   * Deletes the webhook endpoint for the bound account.
   *
   * @param deletion deletion options
   */
  void deleteWebhook(ChannelWebhookDeletion deletion);

  /**
   * Reads the current webhook status from the upstream channel platform.
   *
   * @return current webhook status when the upstream platform returns one
   */
  Optional<ChannelWebhookStatus> getWebhookInfo();
}
