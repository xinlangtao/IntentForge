package cn.intentforge.boot.server;

import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookPropertyNames;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TelegramWebhookServerMainTest {
  @Test
  void shouldResolveSettingsFromSystemPropertiesBeforeEnvironment() {
    TelegramWebhookServerSettings settings = TelegramWebhookServerMain.resolveSettings(
        key -> switch (key) {
          case "intentforge.telegram.accountId" -> "telegram-demo";
          case "intentforge.telegram.displayName" -> "Demo Bot";
          case "intentforge.telegram.botToken" -> "system-token";
          case "intentforge.telegram.webhookBaseUrl" -> "https://hooks.example.com";
          case "intentforge.telegram.webhookAllowedUpdates" -> "message, callback_query";
          default -> null;
        },
        key -> switch (key) {
          case "TG_BOT_TOKEN" -> "env-token";
          case "TG_WEBHOOK_SECRET" -> "env-secret";
          default -> null;
        });

    Assertions.assertEquals("telegram-demo", settings.accountId());
    Assertions.assertEquals("Demo Bot", settings.displayName());
    Assertions.assertEquals("system-token", settings.botToken());
    Assertions.assertEquals("https://hooks.example.com", settings.webhookBaseUrl());
    Assertions.assertEquals("env-secret", settings.webhookSecretToken());
    Assertions.assertEquals("message, callback_query", settings.webhookAllowedUpdates());
    Assertions.assertTrue(settings.webhookAutoManage());
  }

  @Test
  void shouldRejectMissingTelegramBotToken() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> TelegramWebhookServerMain.resolveSettings(key -> null, key -> null));
  }

  @Test
  void shouldRejectInvalidWebhookMaxConnections() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> TelegramWebhookServerMain.resolveSettings(
            key -> switch (key) {
              case "intentforge.telegram.botToken" -> "bot-token";
              case "intentforge.telegram.webhookMaxConnections" -> "abc";
              default -> null;
            },
            key -> null));
  }

  @Test
  void shouldStartServerWithTelegramHookAccountRegistration() throws Exception {
    Path pluginsDirectory = Files.createTempDirectory("telegram-main-plugins");
    TelegramWebhookServerSettings settings = new TelegramWebhookServerSettings(
        "telegram-demo",
        "Demo Bot",
        "bot-token",
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        false);

    try (AiAssetServerRuntime runtime = TelegramWebhookServerMain.startServer(
        new InetSocketAddress("127.0.0.1", 0),
        pluginsDirectory,
        settings)) {
      HttpClient client = HttpClient.newHttpClient();
      HttpResponse<String> response = client.send(
          HttpRequest.newBuilder(runtime.baseUri().resolve("/open-api/hooks/telegram/accounts/telegram-demo/webhook"))
              .timeout(Duration.ofSeconds(10))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString("""
                  {
                    "update_id": 77,
                    "message": {
                      "message_id": 11,
                      "text": "hello telegram main",
                      "chat": {
                        "id": 123456,
                        "type": "private"
                      },
                      "from": {
                        "id": 99,
                        "is_bot": false,
                        "first_name": "Ada"
                      }
                    }
                  }
                  """))
              .build(),
          HttpResponse.BodyHandlers.ofString());

      Assertions.assertEquals(200, response.statusCode());
      Assertions.assertEquals("OK", response.body());
      Assertions.assertTrue(runtime.localRuntime().sessionManager().find("telegram:123456").isPresent());
      Assertions.assertTrue(runtime.localRuntime().channelManager().openWebhookAdministration(settings.toAccountProfile()).isPresent());
      Assertions.assertEquals(ChannelType.TELEGRAM, settings.toAccountProfile().type());
      Assertions.assertEquals("bot-token", settings.toAccountProfile().properties().get("botToken"));
      Assertions.assertEquals("false", settings.toAccountProfile().properties().get(ChannelWebhookPropertyNames.WEBHOOK_AUTO_MANAGE));
    }
  }
}
