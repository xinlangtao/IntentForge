package cn.intentforge.channel.wecom;

import static cn.intentforge.common.util.ValidationSupport.requireText;
import static cn.intentforge.common.util.ValidationSupport.textOrDefault;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelCapability;
import cn.intentforge.channel.ChannelDescriptor;
import cn.intentforge.channel.ChannelDriver;
import cn.intentforge.channel.ChannelSession;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookHandler;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class WeComChannelDriver implements ChannelDriver {
  static final String DRIVER_ID = "intentforge.channel.wecom";
  private static final String DEFAULT_BASE_URL = "https://qyapi.weixin.qq.com";
  private static final ChannelDescriptor DESCRIPTOR = new ChannelDescriptor(
      DRIVER_ID,
      ChannelType.WECOM,
      "WeCom Channel",
      List.of(ChannelCapability.SEND_MESSAGES, ChannelCapability.RECEIVE_MESSAGES),
      Map.of("builtin", "true"));

  private final WeComApplicationApiClient apiClient;

  WeComChannelDriver() {
    this(new HttpWeComApplicationApiClient());
  }

  WeComChannelDriver(WeComApplicationApiClient apiClient) {
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
    String corpId = requireText(accountProfile.properties().get("corpId"), "corpId");
    String agentId = requireText(accountProfile.properties().get("agentId"), "agentId");
    String corpSecret = requireText(accountProfile.properties().get("corpSecret"), "corpSecret");
    String baseUrl = textOrDefault(accountProfile.properties().get("baseUrl"), DEFAULT_BASE_URL);
    return new WeComChannelSession(accountProfile, baseUrl, corpId, agentId, corpSecret, apiClient);
  }

  @Override
  public Optional<ChannelWebhookHandler> openWebhookHandler(ChannelAccountProfile accountProfile) {
    Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    if (accountProfile.type() != ChannelType.WECOM) {
      throw new IllegalArgumentException("unsupported channel type: " + accountProfile.type());
    }
    requireText(accountProfile.properties().get("corpId"), "corpId");
    requireText(accountProfile.properties().get("agentId"), "agentId");
    requireText(accountProfile.properties().get("corpSecret"), "corpSecret");
    return Optional.of(new WeComWebhookHandler(accountProfile));
  }
}
