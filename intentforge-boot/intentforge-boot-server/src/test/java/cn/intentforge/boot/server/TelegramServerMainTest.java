package cn.intentforge.boot.server;

import cn.intentforge.channel.telegram.inbound.TelegramInboundMode;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TelegramServerMainTest {
  @Test
  void shouldDefaultToLongPollingMode() {
    TelegramServerSettings settings = TelegramServerMain.resolveSettings(
        key -> switch (key) {
          case "intentforge.telegram.accountId" -> "telegram-demo";
          case "intentforge.telegram.displayName" -> "Demo Bot";
          case "intentforge.telegram.botToken" -> "system-token";
          default -> null;
        },
        key -> null);

    Assertions.assertEquals(TelegramInboundMode.LONG_POLLING, settings.inboundMode());
  }

  @Test
  void shouldAllowExplicitWebhookModeFromSettings() {
    TelegramServerSettings settings = TelegramServerMain.resolveSettings(
        key -> switch (key) {
          case "intentforge.telegram.botToken" -> "system-token";
          case "intentforge.telegram.inboundMode" -> "WEBHOOK";
          case "intentforge.telegram.webhookBaseUrl" -> "https://hooks.example.com";
          default -> null;
        },
        key -> null);

    Assertions.assertEquals(TelegramInboundMode.WEBHOOK, settings.inboundMode());
    Assertions.assertTrue(settings.webhookAutoManage());
  }

  @Test
  void shouldStartLongPollingByDefaultWithoutRegisteringWebhookRoute() throws Exception {
    Path pluginsDirectory = Files.createTempDirectory("telegram-server-main-long-polling");
    TelegramServerSettings settings = new TelegramServerSettings(
        "telegram-demo",
        "Demo Bot",
        "bot-token",
        null,
        TelegramInboundMode.LONG_POLLING,
        null,
        null,
        null,
        null,
        null,
        false,
        false);
    AtomicBoolean pollingStarterCalled = new AtomicBoolean();

    try (TelegramServerRuntime runtime = TelegramServerMain.startServer(
        new InetSocketAddress("127.0.0.1", 0),
        pluginsDirectory,
        settings,
        (accountProfile, localRuntime) -> {
          pollingStarterCalled.set(true);
          return () -> {
          };
        })) {
      HttpClient client = HttpClient.newHttpClient();
      HttpResponse<String> response = client.send(
          HttpRequest.newBuilder(runtime.serverRuntime().baseUri().resolve("/open-api/hooks/telegram/accounts/telegram-demo/webhook"))
              .timeout(Duration.ofSeconds(10))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString("{}"))
              .build(),
          HttpResponse.BodyHandlers.ofString());

      Assertions.assertTrue(pollingStarterCalled.get());
      Assertions.assertEquals(404, response.statusCode());
    }
  }

  @Test
  void shouldStartWebhookModeWhenExplicitlySelected() throws Exception {
    Path pluginsDirectory = Files.createTempDirectory("telegram-server-main-webhook");
    TelegramServerSettings settings = new TelegramServerSettings(
        "telegram-demo",
        "Demo Bot",
        "bot-token",
        null,
        TelegramInboundMode.WEBHOOK,
        null,
        null,
        null,
        null,
        null,
        false,
        false);
    AtomicBoolean pollingStarterCalled = new AtomicBoolean();

    try (TelegramServerRuntime runtime = TelegramServerMain.startServer(
        new InetSocketAddress("127.0.0.1", 0),
        pluginsDirectory,
        settings,
        (accountProfile, localRuntime) -> {
          pollingStarterCalled.set(true);
          return () -> {
          };
        })) {
      HttpClient client = HttpClient.newHttpClient();
      HttpResponse<String> response = client.send(
          HttpRequest.newBuilder(runtime.serverRuntime().baseUri().resolve("/open-api/hooks/telegram/accounts/telegram-demo/webhook"))
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

      Assertions.assertFalse(pollingStarterCalled.get());
      Assertions.assertEquals(200, response.statusCode());
      Assertions.assertEquals("OK", response.body());
      Assertions.assertTrue(runtime.serverRuntime().localRuntime().sessionManager().find("telegram:123456").isPresent());
    }
  }
}
