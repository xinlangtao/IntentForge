package cn.intentforge.hook;

/**
 * Enumerates managed webhook lifecycle operations executed during hook auto-management.
 *
 * @since 1.0.0
 */
public enum HookWebhookAutoManagementOperation {
  /**
   * Register or update the webhook endpoint.
   */
  REGISTER,
  /**
   * Delete the webhook endpoint.
   */
  DELETE
}
