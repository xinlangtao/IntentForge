package cn.intentforge.channel.telegram.inbound.polling;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelInboundProcessingResult;
import cn.intentforge.channel.ChannelInboundProcessor;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookAdministration;
import cn.intentforge.channel.ChannelWebhookDeletion;
import cn.intentforge.channel.ChannelWebhookRegistration;
import cn.intentforge.channel.ChannelWebhookRequest;
import cn.intentforge.channel.ChannelWebhookResponse;
import cn.intentforge.channel.ChannelWebhookStatus;
import cn.intentforge.channel.telegram.config.TelegramChannelPropertyNames;
import cn.intentforge.channel.telegram.inbound.TelegramInboundMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TelegramLongPollingIngressTest {
  @Test
  void shouldDeleteWebhookOnceAndForwardEachFetchedUpdate() {
    RecordingLongPollingApiClient apiClient = new RecordingLongPollingApiClient(List.of(
        new TelegramFetchedUpdate(10L, """
            {
              "update_id": 10,
              "message": {
                "message_id": 100,
                "text": "hello polling",
                "chat": {
                  "id": 12345,
                  "type": "private"
                },
                "from": {
                  "id": 88,
                  "is_bot": false,
                  "first_name": "Ada"
                }
              }
            }
            """),
        new TelegramFetchedUpdate(12L, """
            {
              "update_id": 12,
              "callback_query": {
                "id": "callback-1",
                "data": "approve",
                "message": {
                  "message_id": 101,
                  "chat": {
                    "id": 12345,
                    "type": "private"
                  }
                },
                "from": {
                  "id": 88,
                  "is_bot": false,
                  "first_name": "Ada"
                }
              }
            }
            """)));
    RecordingInboundProcessor inboundProcessor = new RecordingInboundProcessor();
    RecordingWebhookAdministration administration = new RecordingWebhookAdministration();
    TelegramLongPollingIngress ingress = new TelegramLongPollingIngress(
        accountProfile(Map.of(
            TelegramChannelPropertyNames.BOT_TOKEN, "bot-token",
            TelegramChannelPropertyNames.INBOUND_MODE, TelegramInboundMode.LONG_POLLING.name(),
            TelegramChannelPropertyNames.POLLING_ALLOWED_UPDATES, "message,callback_query")),
        inboundProcessor,
        apiClient,
        administration);

    ingress.prepareStart();
    Long nextOffset = ingress.pollOnce(null);

    Assertions.assertEquals(1, administration.deletions.size());
    Assertions.assertFalse(administration.deletions.getFirst().dropPendingUpdates());
    Assertions.assertEquals("https://api.telegram.org", apiClient.command.baseUrl());
    Assertions.assertEquals("bot-token", apiClient.command.botToken());
    Assertions.assertNull(apiClient.command.offset());
    Assertions.assertEquals(List.of("message", "callback_query"), apiClient.command.allowedUpdates());
    Assertions.assertEquals(2, inboundProcessor.requests.size());
    Assertions.assertEquals("POST", inboundProcessor.requests.getFirst().method());
    Assertions.assertTrue(inboundProcessor.requests.getFirst().body().contains("\"update_id\": 10"));
    Assertions.assertTrue(inboundProcessor.requests.get(1).body().contains("\"callback_query\""));
    Assertions.assertEquals(13L, nextOffset);
  }

  @Test
  void shouldSkipWebhookDeletionWhenPollingDeleteWebhookOnStartIsDisabled() {
    RecordingLongPollingApiClient apiClient = new RecordingLongPollingApiClient(List.of());
    RecordingInboundProcessor inboundProcessor = new RecordingInboundProcessor();
    RecordingWebhookAdministration administration = new RecordingWebhookAdministration();
    TelegramLongPollingIngress ingress = new TelegramLongPollingIngress(
        accountProfile(Map.of(
            TelegramChannelPropertyNames.BOT_TOKEN, "bot-token",
            TelegramChannelPropertyNames.INBOUND_MODE, TelegramInboundMode.LONG_POLLING.name(),
            TelegramChannelPropertyNames.POLLING_DELETE_WEBHOOK_ON_START, "false")),
        inboundProcessor,
        apiClient,
        administration);

    ingress.prepareStart();
    Long nextOffset = ingress.pollOnce(51L);

    Assertions.assertTrue(administration.deletions.isEmpty());
    Assertions.assertEquals(51L, apiClient.command.offset());
    Assertions.assertEquals(51L, nextOffset);
    Assertions.assertTrue(inboundProcessor.requests.isEmpty());
  }

  private static ChannelAccountProfile accountProfile(Map<String, String> properties) {
    return new ChannelAccountProfile("telegram-account", ChannelType.TELEGRAM, "Telegram Bot", properties);
  }

  private static final class RecordingLongPollingApiClient implements TelegramLongPollingApiClient {
    private final List<TelegramFetchedUpdate> updates;
    private TelegramGetUpdatesCommand command;

    private RecordingLongPollingApiClient(List<TelegramFetchedUpdate> updates) {
      this.updates = updates;
    }

    @Override
    public List<TelegramFetchedUpdate> getUpdates(TelegramGetUpdatesCommand command) {
      this.command = command;
      return updates;
    }
  }

  private static final class RecordingInboundProcessor implements ChannelInboundProcessor {
    private final List<ChannelWebhookRequest> requests = new ArrayList<>();

    @Override
    public ChannelInboundProcessingResult process(ChannelAccountProfile accountProfile, ChannelWebhookRequest request) {
      requests.add(request);
      return new ChannelInboundProcessingResult(
          new ChannelWebhookResponse(200, "text/plain; charset=utf-8", "OK", Map.of()),
          List.of());
    }
  }

  private static final class RecordingWebhookAdministration implements ChannelWebhookAdministration {
    private final List<ChannelWebhookDeletion> deletions = new ArrayList<>();

    @Override
    public void setWebhook(ChannelWebhookRegistration registration) {
      throw new UnsupportedOperationException("setWebhook is not used by this test");
    }

    @Override
    public void deleteWebhook(ChannelWebhookDeletion deletion) {
      deletions.add(deletion);
    }

    @Override
    public Optional<ChannelWebhookStatus> getWebhookInfo() {
      return Optional.empty();
    }
  }
}
