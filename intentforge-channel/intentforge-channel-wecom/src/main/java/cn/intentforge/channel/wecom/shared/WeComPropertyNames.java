package cn.intentforge.channel.wecom.shared;

/**
 * Shared WeCom intelligent-robot account property names.
 *
 * @since 1.0.0
 */
public final class WeComPropertyNames {
  /**
   * Optional WeCom API base URL property.
   */
  public static final String BASE_URL = "baseUrl";
  /**
   * Callback signature token property.
   */
  public static final String CALLBACK_TOKEN = "callbackToken";
  /**
   * Callback AES key property.
   */
  public static final String CALLBACK_ENCODING_AES_KEY = "callbackEncodingAesKey";
  /**
   * Optional callback receive-id property.
   */
  public static final String RECEIVE_ID = "receiveId";
  /**
   * Robot identifier property.
   */
  public static final String ROBOT_ID = "robotId";
  /**
   * Robot secret property for outbound API calls.
   */
  public static final String ROBOT_SECRET = "robotSecret";

  private WeComPropertyNames() {
  }
}
