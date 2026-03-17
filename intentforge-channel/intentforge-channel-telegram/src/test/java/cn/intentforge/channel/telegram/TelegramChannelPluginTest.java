package cn.intentforge.channel.telegram;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelCapability;
import cn.intentforge.channel.ChannelDeliveryResult;
import cn.intentforge.channel.ChannelOutboundRequest;
import cn.intentforge.channel.ChannelSession;
import cn.intentforge.channel.ChannelTarget;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.telegram.driver.TelegramChannelDriver;
import cn.intentforge.channel.telegram.outbound.TelegramBotApiClient;
import cn.intentforge.channel.telegram.outbound.TelegramSendCommand;
import cn.intentforge.channel.telegram.outbound.TelegramSendResult;
import cn.intentforge.channel.telegram.plugin.TelegramChannelPlugin;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TelegramChannelPluginTest {
  @Test
  void shouldExposeTelegramDriverDescriptor() {
    TelegramChannelPlugin plugin = new TelegramChannelPlugin();

    Assertions.assertEquals(1, plugin.drivers().size());
    Assertions.assertEquals("intentforge.channel.telegram", plugin.drivers().iterator().next().descriptor().id());
    Assertions.assertEquals(ChannelType.TELEGRAM, plugin.drivers().iterator().next().descriptor().type());
    Assertions.assertTrue(plugin.drivers().iterator().next().descriptor().supports(ChannelCapability.SEND_MESSAGES));
    Assertions.assertTrue(plugin.drivers().iterator().next().descriptor().supports(ChannelCapability.RECEIVE_MESSAGES));
    Assertions.assertTrue(plugin.drivers().iterator().next().descriptor().supports(ChannelCapability.THREAD_REPLIES));
  }

  @Test
  void shouldOpenSessionAndMapOutboundRequestToTelegramCommand() {
    RecordingTelegramBotApiClient apiClient = new RecordingTelegramBotApiClient();
    TelegramChannelDriver driver = new TelegramChannelDriver(apiClient);
    ChannelAccountProfile accountProfile = new ChannelAccountProfile(
        "telegram-account",
        ChannelType.TELEGRAM,
        "Telegram Bot",
        Map.of("botToken", "bot-token"));

    ChannelSession session = driver.openSession(accountProfile);
    ChannelDeliveryResult result = session.send(new ChannelOutboundRequest(
        new ChannelTarget("telegram-account", "-100123", "42", null, Map.of()),
        "hello telegram",
        Map.of(
            "parseMode", "MarkdownV2",
            "disableNotification", Boolean.TRUE,
            "disableWebPagePreview", Boolean.TRUE)));

    Assertions.assertEquals("bot-token", apiClient.command.botToken());
    Assertions.assertEquals("https://api.telegram.org", apiClient.command.baseUrl());
    Assertions.assertEquals("-100123", apiClient.command.chatId());
    Assertions.assertEquals("42", apiClient.command.messageThreadId());
    Assertions.assertEquals("hello telegram", apiClient.command.text());
    Assertions.assertEquals("MarkdownV2", apiClient.command.parseMode());
    Assertions.assertTrue(apiClient.command.disableNotification());
    Assertions.assertTrue(apiClient.command.disableWebPagePreview());
    Assertions.assertEquals("telegram:101", result.deliveryId());
    Assertions.assertEquals("101", result.externalMessageId());
    Assertions.assertEquals(Instant.ofEpochSecond(1_700_000_000L), result.acceptedAt());
  }

  @Test
  void shouldRejectMissingBotToken() {
    TelegramChannelDriver driver = new TelegramChannelDriver(new RecordingTelegramBotApiClient());

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> driver.openSession(new ChannelAccountProfile("telegram-account", ChannelType.TELEGRAM, "Telegram Bot", Map.of())));
  }

  private static final class RecordingTelegramBotApiClient implements TelegramBotApiClient {
    private TelegramSendCommand command;

    @Override
    public TelegramSendResult sendMessage(TelegramSendCommand command) {
      this.command = command;
      return new TelegramSendResult("101", Instant.ofEpochSecond(1_700_000_000L));
    }
  }
}
