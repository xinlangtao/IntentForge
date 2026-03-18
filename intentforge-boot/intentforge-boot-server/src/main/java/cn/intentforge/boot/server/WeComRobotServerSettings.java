package cn.intentforge.boot.server;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.wecom.shared.WeComPropertyNames;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable WeCom intelligent-robot startup settings resolved for the local boot-server entrypoint.
 *
 * @param accountId channel account identifier
 * @param displayName channel display name
 * @param callbackToken callback signature token
 * @param callbackEncodingAesKey callback AES key
 * @param receiveId optional receive-id used by callback decryption validation
 * @param robotId robot identifier
 * @param robotSecret robot secret used by outbound API calls
 * @param baseUrl optional WeCom API base URL override
 * @since 1.0.0
 */
record WeComRobotServerSettings(
    String accountId,
    String displayName,
    String callbackToken,
    String callbackEncodingAesKey,
    String receiveId,
    String robotId,
    String robotSecret,
    String baseUrl
) {
  /**
   * Converts the resolved settings into one hook-visible channel account profile.
   *
   * @return immutable WeCom robot account profile
   */
  ChannelAccountProfile toAccountProfile() {
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put(WeComPropertyNames.CALLBACK_TOKEN, callbackToken);
    properties.put(WeComPropertyNames.CALLBACK_ENCODING_AES_KEY, callbackEncodingAesKey);
    properties.put(WeComPropertyNames.ROBOT_ID, robotId);
    properties.put(WeComPropertyNames.ROBOT_SECRET, robotSecret);
    putIfPresent(properties, WeComPropertyNames.RECEIVE_ID, receiveId);
    putIfPresent(properties, WeComPropertyNames.BASE_URL, baseUrl);
    return new ChannelAccountProfile(accountId, ChannelType.WECOM, displayName, Map.copyOf(properties));
  }

  private static void putIfPresent(Map<String, String> target, String key, String value) {
    if (value != null && !value.isBlank()) {
      target.put(key, value);
    }
  }
}
