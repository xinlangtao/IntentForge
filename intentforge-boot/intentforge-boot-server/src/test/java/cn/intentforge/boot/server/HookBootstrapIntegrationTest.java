package cn.intentforge.boot.server;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelType;
import java.nio.file.Files;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HookBootstrapIntegrationTest {
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
}
