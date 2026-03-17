package cn.intentforge.hook;

import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookStatus;
import java.util.Objects;

/**
 * Records one managed webhook lifecycle operation performed for a hook-visible account.
 *
 * @param accountId target account identifier
 * @param channelType target channel type
 * @param operation executed lifecycle operation
 * @param webhookStatus observed webhook status after the operation when available
 * @since 1.0.0
 */
public record HookWebhookAutoManagementResult(
    String accountId,
    ChannelType channelType,
    HookWebhookAutoManagementOperation operation,
    ChannelWebhookStatus webhookStatus
) {
  /**
   * Creates one validated webhook auto-management result.
   */
  public HookWebhookAutoManagementResult {
    accountId = Objects.requireNonNull(accountId, "accountId must not be null");
    channelType = Objects.requireNonNull(channelType, "channelType must not be null");
    operation = Objects.requireNonNull(operation, "operation must not be null");
  }
}
