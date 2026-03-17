package cn.intentforge.channel.telegram;

import static cn.intentforge.common.util.ValidationSupport.requireText;
import static cn.intentforge.common.util.ValidationSupport.textOrDefault;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelCapability;
import cn.intentforge.channel.ChannelDescriptor;
import cn.intentforge.channel.ChannelDriver;
import cn.intentforge.channel.ChannelSession;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookAdministration;
import cn.intentforge.channel.ChannelWebhookHandler;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class TelegramChannelDriver implements ChannelDriver {
  static final String DRIVER_ID = "intentforge.channel.telegram";
  private static final String DEFAULT_BASE_URL = "https://api.telegram.org";
  private static final ChannelDescriptor DESCRIPTOR = new ChannelDescriptor(
      DRIVER_ID,
      ChannelType.TELEGRAM,
      "Telegram Channel",
      List.of(ChannelCapability.SEND_MESSAGES, ChannelCapability.RECEIVE_MESSAGES, ChannelCapability.THREAD_REPLIES),
      Map.of("builtin", "true"));

  private final TelegramBotApiClient apiClient;
  private final TelegramWebhookApiClient webhookApiClient;

  TelegramChannelDriver() {
    this(new HttpTelegramBotApiClient(), new HttpTelegramWebhookApiClient());
  }

  TelegramChannelDriver(TelegramBotApiClient apiClient) {
    this(apiClient, new HttpTelegramWebhookApiClient());
  }

  TelegramChannelDriver(TelegramBotApiClient apiClient, TelegramWebhookApiClient webhookApiClient) {
    this.apiClient = Objects.requireNonNull(apiClient, "apiClient must not be null");
    this.webhookApiClient = Objects.requireNonNull(webhookApiClient, "webhookApiClient must not be null");
  }

  @Override
  public ChannelDescriptor descriptor() {
    return DESCRIPTOR;
  }

  @Override
  public ChannelSession openSession(ChannelAccountProfile accountProfile) {
    Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    if (accountProfile.type() != ChannelType.TELEGRAM) {
      throw new IllegalArgumentException("unsupported channel type: " + accountProfile.type());
    }
    String botToken = requireText(accountProfile.properties().get("botToken"), "botToken");
    String baseUrl = textOrDefault(accountProfile.properties().get("baseUrl"), DEFAULT_BASE_URL);
    return new TelegramChannelSession(accountProfile, baseUrl, botToken, apiClient);
  }

  @Override
  public Optional<ChannelWebhookHandler> openWebhookHandler(ChannelAccountProfile accountProfile) {
    Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    if (accountProfile.type() != ChannelType.TELEGRAM) {
      throw new IllegalArgumentException("unsupported channel type: " + accountProfile.type());
    }
    requireText(accountProfile.properties().get("botToken"), "botToken");
    return Optional.of(new TelegramWebhookHandler(accountProfile));
  }

  @Override
  public Optional<ChannelWebhookAdministration> openWebhookAdministration(ChannelAccountProfile accountProfile) {
    Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    if (accountProfile.type() != ChannelType.TELEGRAM) {
      throw new IllegalArgumentException("unsupported channel type: " + accountProfile.type());
    }
    String botToken = requireText(accountProfile.properties().get("botToken"), "botToken");
    String baseUrl = textOrDefault(accountProfile.properties().get("baseUrl"), DEFAULT_BASE_URL);
    return Optional.of(new TelegramWebhookAdministration(baseUrl, botToken, webhookApiClient));
  }
}
