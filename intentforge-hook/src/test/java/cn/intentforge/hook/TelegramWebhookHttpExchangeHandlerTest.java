package cn.intentforge.hook;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelInboundDispatch;
import cn.intentforge.channel.ChannelInboundMessage;
import cn.intentforge.channel.ChannelInboundProcessingResult;
import cn.intentforge.channel.ChannelInboundProcessor;
import cn.intentforge.channel.ChannelParticipant;
import cn.intentforge.channel.ChannelRouteDecision;
import cn.intentforge.channel.ChannelTarget;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookResponse;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TelegramWebhookHttpExchangeHandlerTest {
  @Test
  void shouldRouteTelegramSpecificWebhookPathToInboundProcessor() throws Exception {
    InMemoryHookAccountRegistry accountRegistry = new InMemoryHookAccountRegistry();
    accountRegistry.register(new ChannelAccountProfile(
        "telegram-account",
        ChannelType.TELEGRAM,
        "Telegram Bot",
        Map.of("botToken", "bot-token")));
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      HookHttpRouteRegistrar.register(server, new TelegramWebhookHttpExchangeHandler(
          new ChannelWebhookEndpointController(accountRegistry, fixedProcessor())));
      server.start();

      HttpResponse<String> response = HttpClient.newHttpClient().send(
          HttpRequest.newBuilder(URI.create(
                  "http://127.0.0.1:" + server.getAddress().getPort()
                      + "/open-api/hooks/telegram/accounts/telegram-account/webhook"))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString("{\"update_id\":9001}"))
              .build(),
          HttpResponse.BodyHandlers.ofString());

      Assertions.assertEquals(200, response.statusCode());
      Assertions.assertEquals("OK", response.body());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void shouldRejectUnsupportedTelegramSpecificPath() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      HookHttpRouteRegistrar.register(server, new TelegramWebhookHttpExchangeHandler(
          new ChannelWebhookEndpointController(new InMemoryHookAccountRegistry(), fixedProcessor())));
      server.start();

      HttpResponse<String> response = HttpClient.newHttpClient().send(
          HttpRequest.newBuilder(URI.create(
                  "http://127.0.0.1:" + server.getAddress().getPort()
                      + "/open-api/hooks/telegram/telegram-account"))
              .POST(HttpRequest.BodyPublishers.ofString("{}"))
              .build(),
          HttpResponse.BodyHandlers.ofString());

      Assertions.assertEquals(404, response.statusCode());
    } finally {
      server.stop(0);
    }
  }

  private static ChannelInboundProcessor fixedProcessor() {
    return (accountProfile, request) -> new ChannelInboundProcessingResult(
        new ChannelWebhookResponse(200, "text/plain; charset=utf-8", "OK", Map.of()),
        List.of(new ChannelInboundDispatch(
            new ChannelInboundMessage(
                "message-1",
                accountProfile.id(),
                accountProfile.type(),
                new ChannelTarget(accountProfile.id(), "chat-1", null, null, Map.of()),
                new ChannelParticipant("sender-1", "Sender", false, Map.of()),
                "hello hook",
                Map.of("receivedAt", Instant.ofEpochSecond(1_700_000_000L))),
            new cn.intentforge.channel.ChannelAccessDecision(true, "allowed"),
            Optional.of(new ChannelRouteDecision("application-alpha", "session-alpha", null, Map.of("source", "hook"))))));
  }
}
