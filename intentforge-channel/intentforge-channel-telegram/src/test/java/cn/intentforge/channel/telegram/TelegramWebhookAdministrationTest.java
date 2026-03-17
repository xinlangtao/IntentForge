package cn.intentforge.channel.telegram;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookAdministration;
import cn.intentforge.channel.ChannelWebhookDeletion;
import cn.intentforge.channel.ChannelWebhookRegistration;
import cn.intentforge.channel.ChannelWebhookStatus;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TelegramWebhookAdministrationTest {
  @Test
  void shouldOpenWebhookAdministrationAndMapRegistrationToTelegramCommand() {
    RecordingTelegramWebhookApiClient webhookApiClient = new RecordingTelegramWebhookApiClient();
    TelegramChannelDriver driver = new TelegramChannelDriver(
        new RecordingTelegramBotApiClient(),
        webhookApiClient);
    ChannelAccountProfile accountProfile = new ChannelAccountProfile(
        "telegram-account",
        ChannelType.TELEGRAM,
        "Telegram Bot",
        Map.of("botToken", "bot-token"));

    ChannelWebhookAdministration administration = driver.openWebhookAdministration(accountProfile).orElseThrow();
    administration.setWebhook(new ChannelWebhookRegistration(
        URI.create("https://hooks.example.com/open-api/hooks/telegram/accounts/telegram-account/webhook"),
        "secret-token",
        List.of("message", "callback_query"),
        42,
        true,
        Map.of()));

    Assertions.assertEquals("https://api.telegram.org", webhookApiClient.setWebhookCommand.baseUrl());
    Assertions.assertEquals("bot-token", webhookApiClient.setWebhookCommand.botToken());
    Assertions.assertEquals(
        "https://hooks.example.com/open-api/hooks/telegram/accounts/telegram-account/webhook",
        webhookApiClient.setWebhookCommand.webhookUrl().toString());
    Assertions.assertEquals("secret-token", webhookApiClient.setWebhookCommand.secretToken());
    Assertions.assertEquals(List.of("message", "callback_query"), webhookApiClient.setWebhookCommand.allowedUpdates());
    Assertions.assertEquals(42, webhookApiClient.setWebhookCommand.maxConnections());
    Assertions.assertTrue(webhookApiClient.setWebhookCommand.dropPendingUpdates());
  }

  @Test
  void shouldDeleteWebhookAndMapDeletionOptions() {
    RecordingTelegramWebhookApiClient webhookApiClient = new RecordingTelegramWebhookApiClient();
    TelegramChannelDriver driver = new TelegramChannelDriver(
        new RecordingTelegramBotApiClient(),
        webhookApiClient);
    ChannelAccountProfile accountProfile = new ChannelAccountProfile(
        "telegram-account",
        ChannelType.TELEGRAM,
        "Telegram Bot",
        Map.of("botToken", "bot-token", "baseUrl", "https://telegram.internal"));

    ChannelWebhookAdministration administration = driver.openWebhookAdministration(accountProfile).orElseThrow();
    administration.deleteWebhook(new ChannelWebhookDeletion(true, Map.of()));

    Assertions.assertEquals("https://telegram.internal", webhookApiClient.deleteWebhookCommand.baseUrl());
    Assertions.assertEquals("bot-token", webhookApiClient.deleteWebhookCommand.botToken());
    Assertions.assertTrue(webhookApiClient.deleteWebhookCommand.dropPendingUpdates());
  }

  @Test
  void shouldExposeWebhookInfoAsSharedStatus() {
    RecordingTelegramWebhookApiClient webhookApiClient = new RecordingTelegramWebhookApiClient();
    webhookApiClient.webhookInfo = new TelegramWebhookInfo(
        URI.create("https://hooks.example.com/open-api/hooks/telegram/accounts/telegram-account/webhook"),
        7,
        40,
        "149.154.167.220",
        Instant.ofEpochSecond(1_700_000_100L),
        "timeout",
        List.of("message", "callback_query"));
    TelegramChannelDriver driver = new TelegramChannelDriver(
        new RecordingTelegramBotApiClient(),
        webhookApiClient);
    ChannelAccountProfile accountProfile = new ChannelAccountProfile(
        "telegram-account",
        ChannelType.TELEGRAM,
        "Telegram Bot",
        Map.of("botToken", "bot-token"));

    ChannelWebhookAdministration administration = driver.openWebhookAdministration(accountProfile).orElseThrow();
    ChannelWebhookStatus status = administration.getWebhookInfo().orElseThrow();

    Assertions.assertEquals(
        URI.create("https://hooks.example.com/open-api/hooks/telegram/accounts/telegram-account/webhook"),
        status.url());
    Assertions.assertEquals("7", status.metadata().get("pendingUpdateCount"));
    Assertions.assertEquals("40", status.metadata().get("maxConnections"));
    Assertions.assertEquals("149.154.167.220", status.metadata().get("ipAddress"));
    Assertions.assertEquals("timeout", status.metadata().get("lastErrorMessage"));
    Assertions.assertEquals("1700000100", status.metadata().get("lastErrorAt"));
    Assertions.assertEquals("message,callback_query", status.metadata().get("eventTypes"));
  }

  private static final class RecordingTelegramBotApiClient implements TelegramBotApiClient {
    @Override
    public TelegramSendResult sendMessage(TelegramSendCommand command) {
      return new TelegramSendResult("101", Instant.ofEpochSecond(1_700_000_000L));
    }
  }

  private static final class RecordingTelegramWebhookApiClient implements TelegramWebhookApiClient {
    private TelegramSetWebhookCommand setWebhookCommand;
    private TelegramDeleteWebhookCommand deleteWebhookCommand;
    private TelegramWebhookInfo webhookInfo;

    @Override
    public void setWebhook(TelegramSetWebhookCommand command) {
      this.setWebhookCommand = command;
    }

    @Override
    public void deleteWebhook(TelegramDeleteWebhookCommand command) {
      this.deleteWebhookCommand = command;
    }

    @Override
    public TelegramWebhookInfo getWebhookInfo(TelegramWebhookInfoCommand command) {
      return webhookInfo;
    }
  }
}
