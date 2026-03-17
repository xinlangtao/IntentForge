package cn.intentforge.boot.server;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HookBootstrapIntegrationTest {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void shouldExposeGenericChannelHookRouteForTelegram() throws Exception {
    try (AiAssetServerRuntime runtime = AiAssetServerBootstrap.bootstrap(
        Files.createTempDirectory("boot-server-hook-plugins"),
        null,
        null,
        hookAccounts -> hookAccounts.register(new ChannelAccountProfile(
            "telegram-account",
            ChannelType.TELEGRAM,
            "Telegram Bot",
            Map.of("botToken", "bot-token"))))) {
      java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
      java.net.http.HttpResponse<String> response = client.send(
          java.net.http.HttpRequest.newBuilder(
                  runtime.baseUri().resolve("/open-api/hooks/channels/telegram/accounts/telegram-account/webhook"))
              .header("Content-Type", "application/json")
              .POST(java.net.http.HttpRequest.BodyPublishers.ofString("""
                  {
                    "update_id": 9001,
                    "message": {
                      "message_id": 42,
                      "text": "hello webhook",
                      "chat": {
                        "id": -100123,
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
          java.net.http.HttpResponse.BodyHandlers.ofString());

      Assertions.assertEquals(200, response.statusCode());
      Assertions.assertEquals("OK", response.body());
      Assertions.assertTrue(runtime.localRuntime().sessionManager().find("telegram:-100123").isPresent());
    }
  }

  @Test
  void shouldExposeTelegramSpecificHookRoute() throws Exception {
    try (AiAssetServerRuntime runtime = AiAssetServerBootstrap.bootstrap(
        Files.createTempDirectory("boot-server-hook-plugins"),
        null,
        null,
        hookAccounts -> hookAccounts.register(new ChannelAccountProfile(
            "telegram-account",
            ChannelType.TELEGRAM,
            "Telegram Bot",
            Map.of("botToken", "bot-token"))))) {
      java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
      java.net.http.HttpResponse<String> response = client.send(
          java.net.http.HttpRequest.newBuilder(
                  runtime.baseUri().resolve("/open-api/hooks/telegram/accounts/telegram-account/webhook"))
              .header("Content-Type", "application/json")
              .POST(java.net.http.HttpRequest.BodyPublishers.ofString("""
                  {
                    "update_id": 9001,
                    "message": {
                      "message_id": 42,
                      "text": "hello webhook",
                      "chat": {
                        "id": -100123,
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
          java.net.http.HttpResponse.BodyHandlers.ofString());

      Assertions.assertEquals(200, response.statusCode());
      Assertions.assertEquals("OK", response.body());
      Assertions.assertTrue(runtime.localRuntime().sessionManager().find("telegram:-100123").isPresent());
    }
  }

  @Test
  void shouldExposeWeComSpecificCallbackRoute() throws Exception {
    try (AiAssetServerRuntime runtime = AiAssetServerBootstrap.bootstrap(
        Files.createTempDirectory("boot-server-hook-plugins"),
        null,
        null,
        hookAccounts -> hookAccounts.register(new ChannelAccountProfile(
            "wecom-account",
            ChannelType.WECOM,
            "WeCom App",
            Map.of(
                "corpId", "corp-id",
                "agentId", "1000001",
                "corpSecret", "corp-secret"))))) {
      java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
      java.net.http.HttpResponse<String> response = client.send(
          java.net.http.HttpRequest.newBuilder(
                  runtime.baseUri().resolve("/open-api/hooks/wecom/accounts/wecom-account/callback"
                      + "?msg_signature=signature&timestamp=1710000000&nonce=random&echostr=hello%20world"))
              .GET()
              .build(),
          java.net.http.HttpResponse.BodyHandlers.ofString());

      Assertions.assertEquals(200, response.statusCode());
      Assertions.assertEquals("hello world", response.body());
    }
  }

  @Test
  void shouldAutomaticallyManageTelegramWebhookAtStartup() throws Exception {
    try (RecordingTelegramApiServer telegramApiServer = new RecordingTelegramApiServer()) {
      try (AiAssetServerRuntime runtime = AiAssetServerBootstrap.bootstrap(
          Files.createTempDirectory("boot-server-hook-plugins"),
          null,
          null,
          hookAccounts -> hookAccounts.register(new ChannelAccountProfile(
              "telegram-account",
              ChannelType.TELEGRAM,
              "Telegram Bot",
              Map.of(
                  "botToken", "bot-token",
                  "baseUrl", telegramApiServer.baseUri().toString(),
                  "webhookAutoManage", "true",
                  "webhookBaseUrl", "https://hooks.example.com",
                  "webhookSecretToken", "secret-token",
                  "webhookAllowedUpdates", "message, callback_query",
                  "webhookMaxConnections", "42",
                  "webhookDropPendingUpdates", "true"))))) {
        JsonNode setWebhookRequest = OBJECT_MAPPER.readTree(telegramApiServer.lastSetWebhookBody());
        Assertions.assertEquals(
            "https://hooks.example.com/open-api/hooks/telegram/accounts/telegram-account/webhook",
            setWebhookRequest.path("url").asText());
        Assertions.assertEquals("secret-token", setWebhookRequest.path("secret_token").asText());
        Assertions.assertTrue(setWebhookRequest.path("drop_pending_updates").asBoolean());
        Assertions.assertEquals(42, setWebhookRequest.path("max_connections").asInt());
        Assertions.assertEquals("message", setWebhookRequest.path("allowed_updates").get(0).asText());
        Assertions.assertEquals("callback_query", setWebhookRequest.path("allowed_updates").get(1).asText());
        Assertions.assertEquals(1, telegramApiServer.setWebhookCalls());
        Assertions.assertEquals(1, telegramApiServer.getWebhookInfoCalls());
        Assertions.assertNotNull(runtime.baseUri());
      }
    }
  }

  private static final class RecordingTelegramApiServer implements AutoCloseable {
    private final HttpServer server;
    private final AtomicReference<String> lastSetWebhookBody = new AtomicReference<>();
    private final AtomicInteger setWebhookCalls = new AtomicInteger();
    private final AtomicInteger getWebhookInfoCalls = new AtomicInteger();

    private RecordingTelegramApiServer() throws IOException {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/botbot-token/setWebhook", exchange -> {
        setWebhookCalls.incrementAndGet();
        lastSetWebhookBody.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        byte[] responseBody = """
            {"ok":true,"result":true,"description":"Webhook was set"}
            """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBody.length);
        try (OutputStream response = exchange.getResponseBody()) {
          response.write(responseBody);
        }
      });
      server.createContext("/botbot-token/getWebhookInfo", exchange -> {
        getWebhookInfoCalls.incrementAndGet();
        byte[] responseBody = """
            {"ok":true,"result":{"url":"https://hooks.example.com/open-api/hooks/telegram/accounts/telegram-account/webhook","pending_update_count":0,"allowed_updates":["message","callback_query"],"max_connections":42}}
            """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBody.length);
        try (OutputStream response = exchange.getResponseBody()) {
          response.write(responseBody);
        }
      });
      server.start();
    }

    private java.net.URI baseUri() {
      return java.net.URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    private String lastSetWebhookBody() {
      long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
      while (lastSetWebhookBody.get() == null && System.nanoTime() < deadline) {
        Thread.onSpinWait();
      }
      return lastSetWebhookBody.get();
    }

    private int setWebhookCalls() {
      return setWebhookCalls.get();
    }

    private int getWebhookInfoCalls() {
      return getWebhookInfoCalls.get();
    }

    @Override
    public void close() {
      server.stop(0);
    }
  }
}
