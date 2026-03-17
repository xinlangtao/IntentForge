package cn.intentforge.channel.telegram.driver;

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
import cn.intentforge.channel.telegram.admin.HttpTelegramWebhookApiClient;
import cn.intentforge.channel.telegram.admin.TelegramWebhookAdministration;
import cn.intentforge.channel.telegram.admin.TelegramWebhookApiClient;
import cn.intentforge.channel.telegram.inbound.webhook.TelegramWebhookHandler;
import cn.intentforge.channel.telegram.outbound.HttpTelegramBotApiClient;
import cn.intentforge.channel.telegram.outbound.TelegramBotApiClient;
import cn.intentforge.channel.telegram.outbound.TelegramChannelSession;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Pluggable Telegram channel driver.
 *
 * @since 1.0.0
 */
public final class TelegramChannelDriver implements ChannelDriver {
  public static final String DRIVER_ID = "intentforge.channel.telegram";
  private static final String DEFAULT_BASE_URL = "https://api.telegram.org";
  private static final ChannelDescriptor DESCRIPTOR = new ChannelDescriptor(
      DRIVER_ID,
      ChannelType.TELEGRAM,
      "Telegram Channel",
      List.of(ChannelCapability.SEND_MESSAGES, ChannelCapability.RECEIVE_MESSAGES, ChannelCapability.THREAD_REPLIES),
      Map.of("builtin", "true"));

  private final TelegramBotApiClient apiClient;
  private final TelegramWebhookApiClient webhookApiClient;

  /**
   * Creates one Telegram driver with the default outbound and webhook API clients.
   */
  public TelegramChannelDriver() {
    this(new HttpTelegramBotApiClient(), new HttpTelegramWebhookApiClient());
  }

  /**
   * Creates one Telegram driver with a custom outbound API client.
   *
   * @param apiClient outbound Telegram API client
   */
  public TelegramChannelDriver(TelegramBotApiClient apiClient) {
    this(apiClient, new HttpTelegramWebhookApiClient());
  }

  /**
   * Creates one Telegram driver with custom outbound and webhook API clients.
   *
   * @param apiClient outbound Telegram API client
   * @param webhookApiClient webhook administration API client
   */
  public TelegramChannelDriver(TelegramBotApiClient apiClient, TelegramWebhookApiClient webhookApiClient) {
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
