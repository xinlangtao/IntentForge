package cn.intentforge.channel.telegram;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelInboundMessage;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookHandler;
import cn.intentforge.channel.ChannelWebhookRequest;
import cn.intentforge.channel.ChannelWebhookResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TelegramInboundWebhookHandlerTest {
  @Test
  void shouldParseTextUpdateIntoChannelInboundMessage() {
    TelegramChannelDriver driver = new TelegramChannelDriver(new NoOpTelegramBotApiClient());
    ChannelAccountProfile accountProfile = new ChannelAccountProfile(
        "telegram-account",
        ChannelType.TELEGRAM,
        "Telegram Bot",
        Map.of("botToken", "bot-token"));

    ChannelWebhookHandler webhookHandler = driver.openWebhookHandler(accountProfile).orElseThrow();
    ChannelWebhookResult result = webhookHandler.handle(new ChannelWebhookRequest(
        "POST",
        Map.of("Content-Type", List.of("application/json")),
        Map.of(),
        """
            {
              "update_id": 9001,
              "message": {
                "message_id": 42,
                "message_thread_id": 7,
                "date": 1700000000,
                "text": "/hello",
                "chat": {
                  "id": -100123,
                  "type": "supergroup",
                  "title": "Dev Group"
                },
                "from": {
                  "id": 99,
                  "is_bot": false,
                  "first_name": "Ada",
                  "last_name": "Lovelace",
                  "username": "ada"
                }
              }
            }
            """));

    Assertions.assertEquals(200, result.response().statusCode());
    Assertions.assertEquals("OK", result.response().body());
    Assertions.assertEquals(1, result.messages().size());
    ChannelInboundMessage message = result.messages().getFirst();
    Assertions.assertEquals("-100123:42", message.messageId());
    Assertions.assertEquals("telegram-account", message.accountId());
    Assertions.assertEquals(ChannelType.TELEGRAM, message.type());
    Assertions.assertEquals("-100123", message.target().conversationId());
    Assertions.assertEquals("7", message.target().threadId());
    Assertions.assertEquals("99", message.sender().id());
    Assertions.assertEquals("Ada Lovelace", message.sender().displayName());
    Assertions.assertEquals("/hello", message.text());
    Assertions.assertEquals(9001L, message.metadata().get("updateId"));
    Assertions.assertEquals(Instant.ofEpochSecond(1_700_000_000L), message.metadata().get("messageCreatedAt"));
  }

  @Test
  void shouldIgnoreUpdateWithoutTextMessage() {
    TelegramChannelDriver driver = new TelegramChannelDriver(new NoOpTelegramBotApiClient());
    ChannelAccountProfile accountProfile = new ChannelAccountProfile(
        "telegram-account",
        ChannelType.TELEGRAM,
        "Telegram Bot",
        Map.of("botToken", "bot-token"));

    ChannelWebhookHandler webhookHandler = driver.openWebhookHandler(accountProfile).orElseThrow();
    ChannelWebhookResult result = webhookHandler.handle(new ChannelWebhookRequest(
        "POST",
        Map.of(),
        Map.of(),
        """
            {
              "update_id": 9002,
              "message": {
                "message_id": 43,
                "chat": {
                  "id": -100123,
                  "type": "supergroup"
                },
                "from": {
                  "id": 99,
                  "is_bot": false,
                  "first_name": "Ada"
                }
              }
            }
            """));

    Assertions.assertEquals(200, result.response().statusCode());
    Assertions.assertEquals("OK", result.response().body());
    Assertions.assertTrue(result.messages().isEmpty());
  }

  @Test
  void shouldRejectMalformedWebhookPayload() {
    TelegramChannelDriver driver = new TelegramChannelDriver(new NoOpTelegramBotApiClient());
    ChannelAccountProfile accountProfile = new ChannelAccountProfile(
        "telegram-account",
        ChannelType.TELEGRAM,
        "Telegram Bot",
        Map.of("botToken", "bot-token"));

    ChannelWebhookHandler webhookHandler = driver.openWebhookHandler(accountProfile).orElseThrow();

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> webhookHandler.handle(new ChannelWebhookRequest("POST", Map.of(), Map.of(), "{invalid")));
  }

  private static final class NoOpTelegramBotApiClient implements TelegramBotApiClient {
    @Override
    public TelegramSendResult sendMessage(TelegramSendCommand command) {
      return new TelegramSendResult("unused", Instant.EPOCH);
    }
  }
}
