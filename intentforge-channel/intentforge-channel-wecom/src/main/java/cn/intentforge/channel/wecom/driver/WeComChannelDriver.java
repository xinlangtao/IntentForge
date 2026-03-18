package cn.intentforge.channel.wecom.driver;

import static cn.intentforge.common.util.ValidationSupport.requireText;
import static cn.intentforge.common.util.ValidationSupport.textOrDefault;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelCapability;
import cn.intentforge.channel.ChannelDescriptor;
import cn.intentforge.channel.ChannelDriver;
import cn.intentforge.channel.ChannelSession;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookHandler;
import cn.intentforge.channel.wecom.inbound.callback.WeComWebhookHandler;
import cn.intentforge.channel.wecom.outbound.HttpWeComRobotApiClient;
import cn.intentforge.channel.wecom.outbound.WeComChannelSession;
import cn.intentforge.channel.wecom.outbound.WeComRobotApiClient;
import cn.intentforge.channel.wecom.shared.WeComPropertyNames;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Pluggable WeCom intelligent-robot channel driver.
 *
 * @since 1.0.0
 */
public final class WeComChannelDriver implements ChannelDriver {
  /**
   * Stable builtin driver identifier.
   */
  public static final String DRIVER_ID = "intentforge.channel.wecom";
  private static final String DEFAULT_BASE_URL = "https://qyapi.weixin.qq.com";
  private static final ChannelDescriptor DESCRIPTOR = new ChannelDescriptor(
      DRIVER_ID,
      ChannelType.WECOM,
      "WeCom Intelligent Robot",
      List.of(ChannelCapability.SEND_MESSAGES, ChannelCapability.RECEIVE_MESSAGES),
      Map.of("builtin", "true"));

  private final WeComRobotApiClient apiClient;

  /**
   * Creates one driver backed by the default HTTP API client.
   */
  public WeComChannelDriver() {
    this(new HttpWeComRobotApiClient());
  }

  /**
   * Creates one driver with a custom outbound API client.
   *
   * @param apiClient outbound API client
   */
  public WeComChannelDriver(WeComRobotApiClient apiClient) {
    this.apiClient = Objects.requireNonNull(apiClient, "apiClient must not be null");
  }

  @Override
  public ChannelDescriptor descriptor() {
    return DESCRIPTOR;
  }

  @Override
  public ChannelSession openSession(ChannelAccountProfile accountProfile) {
    Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    if (accountProfile.type() != ChannelType.WECOM) {
      throw new IllegalArgumentException("unsupported channel type: " + accountProfile.type());
    }
    String robotId = requireText(accountProfile.properties().get(WeComPropertyNames.ROBOT_ID), WeComPropertyNames.ROBOT_ID);
    String robotSecret = requireText(
        accountProfile.properties().get(WeComPropertyNames.ROBOT_SECRET),
        WeComPropertyNames.ROBOT_SECRET);
    String baseUrl = textOrDefault(accountProfile.properties().get(WeComPropertyNames.BASE_URL), DEFAULT_BASE_URL);
    return new WeComChannelSession(accountProfile, baseUrl, robotId, robotSecret, apiClient);
  }

  @Override
  public Optional<ChannelWebhookHandler> openWebhookHandler(ChannelAccountProfile accountProfile) {
    Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    if (accountProfile.type() != ChannelType.WECOM) {
      throw new IllegalArgumentException("unsupported channel type: " + accountProfile.type());
    }
    requireText(accountProfile.properties().get(WeComPropertyNames.CALLBACK_TOKEN), WeComPropertyNames.CALLBACK_TOKEN);
    requireText(
        accountProfile.properties().get(WeComPropertyNames.CALLBACK_ENCODING_AES_KEY),
        WeComPropertyNames.CALLBACK_ENCODING_AES_KEY);
    return Optional.of(new WeComWebhookHandler(accountProfile));
  }
}
