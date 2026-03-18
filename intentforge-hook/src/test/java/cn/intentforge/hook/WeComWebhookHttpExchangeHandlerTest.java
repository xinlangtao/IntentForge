package cn.intentforge.hook;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelInboundProcessingResult;
import cn.intentforge.channel.ChannelWebhookResponse;
import cn.intentforge.channel.ChannelType;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WeComWebhookHttpExchangeHandlerTest {
  @Test
  void shouldRouteWeComSpecificCallbackPathToInboundProcessor() throws Exception {
    InMemoryHookAccountRegistry accountRegistry = new InMemoryHookAccountRegistry();
    accountRegistry.register(new ChannelAccountProfile(
        "wecom-account",
        ChannelType.WECOM,
        "WeCom Robot",
        Map.of(
            "callbackToken", "robot-token",
            "callbackEncodingAesKey", "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG",
            "robotId", "robot-123",
            "robotSecret", "robot-secret")));
    AtomicReference<String> echo = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      HookHttpRouteRegistrar.register(server, new WeComWebhookHttpExchangeHandler(
          new ChannelWebhookEndpointController(
              accountRegistry,
              (accountProfile, request) -> {
                echo.set(request.firstQueryParameter("echostr"));
                return new ChannelInboundProcessingResult(
                    new ChannelWebhookResponse(200, "text/plain; charset=utf-8", request.firstQueryParameter("echostr"), Map.of()),
                    List.of());
              })));
      server.start();

      HttpResponse<String> response = HttpClient.newHttpClient().send(
          HttpRequest.newBuilder(URI.create(
                  "http://127.0.0.1:" + server.getAddress().getPort()
                      + "/open-api/hooks/wecom/accounts/wecom-account/callback"
                      + "?msg_signature=signature&timestamp=1710000000&nonce=random&echostr=hello%20world"))
              .GET()
              .build(),
          HttpResponse.BodyHandlers.ofString());

      Assertions.assertEquals(200, response.statusCode());
      Assertions.assertEquals("hello world", response.body());
      Assertions.assertEquals("hello world", echo.get());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void shouldRejectUnsupportedWeComSpecificPath() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      HookHttpRouteRegistrar.register(server, new WeComWebhookHttpExchangeHandler(
          new ChannelWebhookEndpointController(new InMemoryHookAccountRegistry(), (accountProfile, request) -> {
            throw new AssertionError("should not be called");
          })));
      server.start();

      HttpResponse<String> response = HttpClient.newHttpClient().send(
          HttpRequest.newBuilder(URI.create(
                  "http://127.0.0.1:" + server.getAddress().getPort()
                      + "/open-api/hooks/wecom/accounts/wecom-account/webhook"))
              .GET()
              .build(),
          HttpResponse.BodyHandlers.ofString());

      Assertions.assertEquals(404, response.statusCode());
    } finally {
      server.stop(0);
    }
  }
}
