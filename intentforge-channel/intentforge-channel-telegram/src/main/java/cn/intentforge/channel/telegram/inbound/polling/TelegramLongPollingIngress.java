package cn.intentforge.channel.telegram.inbound.polling;

import static cn.intentforge.common.util.ValidationSupport.requireText;
import static cn.intentforge.common.util.ValidationSupport.textOrDefault;
import static cn.intentforge.channel.ChannelWebhookPropertyNames.WEBHOOK_DROP_PENDING_UPDATES;
import static cn.intentforge.channel.ChannelWebhookPropertyNames.WEBHOOK_EVENT_TYPES;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelInboundMessageProcessor;
import cn.intentforge.channel.ChannelInboundSource;
import cn.intentforge.channel.ChannelInboundSourceType;
import cn.intentforge.channel.ChannelWebhookAdministration;
import cn.intentforge.channel.ChannelWebhookDeletion;
import cn.intentforge.channel.telegram.config.TelegramChannelPropertyNames;
import cn.intentforge.channel.telegram.inbound.common.TelegramInboundUpdateNormalizer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background Telegram inbound ingress that consumes updates through long polling and forwards them into the
 * shared channel inbound pipeline.
 *
 * @since 1.0.0
 */
public final class TelegramLongPollingIngress implements AutoCloseable {
  static final String DEFAULT_BASE_URL = "https://api.telegram.org";
  static final int DEFAULT_LIMIT = 100;
  static final int DEFAULT_TIMEOUT_SECONDS = 10;
  static final long DEFAULT_IDLE_MILLIS = 1_000L;

  private final ChannelAccountProfile accountProfile;
  private final ChannelInboundMessageProcessor inboundMessageProcessor;
  private final TelegramLongPollingApiClient apiClient;
  private final ChannelWebhookAdministration webhookAdministration;
  private final TelegramInboundUpdateNormalizer normalizer;
  private final AtomicBoolean closed = new AtomicBoolean();

  private volatile Thread worker;

  /**
   * Creates one long-polling ingress with the default Telegram Bot API client.
   *
   * @param accountProfile Telegram account profile
   * @param inboundMessageProcessor shared inbound message processor
   */
  public TelegramLongPollingIngress(ChannelAccountProfile accountProfile, ChannelInboundMessageProcessor inboundMessageProcessor) {
    this(accountProfile, inboundMessageProcessor, new HttpTelegramLongPollingApiClient(), null, new TelegramInboundUpdateNormalizer());
  }

  /**
   * Creates one long-polling ingress with optional webhook administration support.
   *
   * @param accountProfile Telegram account profile
   * @param inboundMessageProcessor shared inbound message processor
   * @param webhookAdministration optional webhook administration used to disable an existing webhook before polling
   */
  public TelegramLongPollingIngress(
      ChannelAccountProfile accountProfile,
      ChannelInboundMessageProcessor inboundMessageProcessor,
      ChannelWebhookAdministration webhookAdministration
  ) {
    this(
        accountProfile,
        inboundMessageProcessor,
        new HttpTelegramLongPollingApiClient(),
        webhookAdministration,
        new TelegramInboundUpdateNormalizer());
  }

  TelegramLongPollingIngress(
      ChannelAccountProfile accountProfile,
      ChannelInboundMessageProcessor inboundMessageProcessor,
      TelegramLongPollingApiClient apiClient,
      ChannelWebhookAdministration webhookAdministration,
      TelegramInboundUpdateNormalizer normalizer
  ) {
    this.accountProfile = Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    this.inboundMessageProcessor = Objects.requireNonNull(inboundMessageProcessor, "inboundMessageProcessor must not be null");
    this.apiClient = Objects.requireNonNull(apiClient, "apiClient must not be null");
    this.webhookAdministration = webhookAdministration;
    this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
  }

  /**
   * Starts the background long-polling loop.
   */
  public synchronized void start() {
    if (worker != null) {
      return;
    }
    prepareStart();
    worker = Thread.ofVirtual().name("telegram-long-polling-" + accountProfile.id()).start(() -> {
      Long offset = null;
      while (!closed.get()) {
        try {
          Long nextOffset = pollOnce(offset);
          boolean idle = Objects.equals(offset, nextOffset);
          offset = nextOffset;
          if (idle) {
            Thread.sleep(DEFAULT_IDLE_MILLIS);
          }
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          break;
        } catch (RuntimeException exception) {
          if (closed.get()) {
            break;
          }
          try {
            Thread.sleep(DEFAULT_IDLE_MILLIS);
          } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    });
  }

  void prepareStart() {
    if (webhookAdministration == null || !deleteWebhookOnStart()) {
      return;
    }
    webhookAdministration.deleteWebhook(new ChannelWebhookDeletion(
        Boolean.parseBoolean(accountProfile.properties().getOrDefault(WEBHOOK_DROP_PENDING_UPDATES, "false")),
        Map.of("accountId", accountProfile.id(), "channelType", accountProfile.type().name())));
  }

  Long pollOnce(Long currentOffset) {
    TelegramGetUpdatesCommand command = new TelegramGetUpdatesCommand(
        textOrDefault(accountProfile.properties().get(TelegramChannelPropertyNames.BASE_URL), DEFAULT_BASE_URL),
        requireText(accountProfile.properties().get(TelegramChannelPropertyNames.BOT_TOKEN), TelegramChannelPropertyNames.BOT_TOKEN),
        currentOffset,
        DEFAULT_LIMIT,
        DEFAULT_TIMEOUT_SECONDS,
        allowedUpdates());
    List<TelegramFetchedUpdate> updates = apiClient.getUpdates(command);
    long nextOffset = currentOffset == null ? 0L : currentOffset;
    boolean advanced = false;
    for (TelegramFetchedUpdate update : updates) {
      inboundMessageProcessor.process(
          accountProfile,
          new ChannelInboundSource(ChannelInboundSourceType.LONG_POLLING, Map.of("updateId", update.updateId())),
          normalizer.normalize(accountProfile, update.payload()));
      nextOffset = Math.max(nextOffset, update.updateId() + 1L);
      advanced = true;
    }
    return advanced ? nextOffset : currentOffset;
  }

  /**
   * Stops the background long-polling loop.
   */
  @Override
  public synchronized void close() {
    closed.set(true);
    if (worker != null) {
      worker.interrupt();
      if (worker != Thread.currentThread()) {
        try {
          worker.join(1_000L);
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
        }
      }
      worker = null;
    }
  }

  private List<String> allowedUpdates() {
    String configuredAllowedUpdates = accountProfile.properties().get(TelegramChannelPropertyNames.POLLING_ALLOWED_UPDATES);
    if (configuredAllowedUpdates == null || configuredAllowedUpdates.isBlank()) {
      configuredAllowedUpdates = accountProfile.properties().get(WEBHOOK_EVENT_TYPES);
    }
    if (configuredAllowedUpdates == null || configuredAllowedUpdates.isBlank()) {
      return List.of();
    }
    return List.of(configuredAllowedUpdates.split(","))
        .stream()
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .toList();
  }

  private boolean deleteWebhookOnStart() {
    return !"false".equalsIgnoreCase(accountProfile.properties().get(TelegramChannelPropertyNames.POLLING_DELETE_WEBHOOK_ON_START));
  }
}
