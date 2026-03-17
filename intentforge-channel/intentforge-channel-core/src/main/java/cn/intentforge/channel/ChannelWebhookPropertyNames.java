package cn.intentforge.channel;

/**
 * Shared account-property names used by channel webhook registration and management.
 *
 * @since 1.0.0
 */
public final class ChannelWebhookPropertyNames {
  /**
   * Account property that enables startup-time automatic webhook lifecycle management.
   */
  public static final String WEBHOOK_AUTO_MANAGE = "webhookAutoManage";
  /**
   * Account property that overrides the full externally reachable webhook URL.
   */
  public static final String WEBHOOK_URL = "webhookUrl";
  /**
   * Account property that provides the externally reachable webhook base URL.
   */
  public static final String WEBHOOK_BASE_URL = "webhookBaseUrl";
  /**
   * Account property that stores the webhook secret token.
   */
  public static final String WEBHOOK_SECRET_TOKEN = "webhookSecretToken";
  /**
   * Account property that lists subscribed webhook event types as a comma-separated string.
   */
  public static final String WEBHOOK_EVENT_TYPES = "webhookAllowedUpdates";
  /**
   * Account property that defines the desired managed webhook state.
   */
  public static final String WEBHOOK_DESIRED_STATE = "webhookDesiredState";
  /**
   * Account property that configures the maximum number of concurrent upstream webhook connections.
   */
  public static final String WEBHOOK_MAX_CONNECTIONS = "webhookMaxConnections";
  /**
   * Account property that controls whether pending updates are dropped during registration or deletion.
   */
  public static final String WEBHOOK_DROP_PENDING_UPDATES = "webhookDropPendingUpdates";

  private ChannelWebhookPropertyNames() {
  }
}
