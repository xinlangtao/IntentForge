package cn.intentforge.hook;

import cn.intentforge.channel.ChannelInboundProcessingResult;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookRequest;
import cn.intentforge.channel.ChannelWebhookResponse;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

final class HookHttpExchangeSupport {
  private HookHttpExchangeSupport() {
  }

  static void handle(
      HttpExchange exchange,
      ChannelWebhookEndpointController controller,
      Function<String, ResolvedRoute> routeParser
  ) throws IOException {
    Objects.requireNonNull(exchange, "exchange must not be null");
    Objects.requireNonNull(controller, "controller must not be null");
    Objects.requireNonNull(routeParser, "routeParser must not be null");
    try {
      ResolvedRoute route = routeParser.apply(exchange.getRequestURI().getPath());
      ChannelInboundProcessingResult result = controller.handle(
          route.channelType(),
          route.accountId(),
          new ChannelWebhookRequest(
              exchange.getRequestMethod(),
              headers(exchange.getRequestHeaders()),
              queryParameters(exchange.getRequestURI().getRawQuery()),
              readBody(exchange)));
      writeResponse(exchange, result.response());
    } catch (HookEndpointException ex) {
      writeResponse(exchange, new ChannelWebhookResponse(
          ex.statusCode(),
          "text/plain; charset=utf-8",
          ex.getMessage(),
          Map.of()));
    } catch (RuntimeException ex) {
      writeResponse(exchange, new ChannelWebhookResponse(
          500,
          "text/plain; charset=utf-8",
          "internal server error",
          Map.of()));
    } finally {
      exchange.close();
    }
  }

  static ResolvedRoute resolvedRoute(ChannelType channelType, String accountId) {
    ChannelType nonNullChannelType = Objects.requireNonNull(channelType, "channelType must not be null");
    String nonBlankAccountId = Objects.requireNonNull(accountId, "accountId must not be null");
    if (nonBlankAccountId.isBlank()) {
      throw new HookEndpointException(404, "hook path not found");
    }
    return new ResolvedRoute(nonNullChannelType, nonBlankAccountId);
  }

  private static String readBody(HttpExchange exchange) throws IOException {
    return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
  }

  private static Map<String, List<String>> headers(Headers headers) {
    Map<String, List<String>> copied = new LinkedHashMap<>();
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      copied.put(entry.getKey(), List.copyOf(entry.getValue()));
    }
    return Map.copyOf(copied);
  }

  private static Map<String, List<String>> queryParameters(String rawQuery) {
    if (rawQuery == null || rawQuery.isBlank()) {
      return Map.of();
    }
    Map<String, List<String>> values = new LinkedHashMap<>();
    for (String pair : rawQuery.split("&")) {
      if (pair.isBlank()) {
        continue;
      }
      String[] parts = pair.split("=", 2);
      String key = decode(parts[0]);
      String value = parts.length == 2 ? decode(parts[1]) : "";
      values.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
    }
    Map<String, List<String>> copied = new LinkedHashMap<>();
    for (Map.Entry<String, List<String>> entry : values.entrySet()) {
      copied.put(entry.getKey(), List.copyOf(entry.getValue()));
    }
    return Map.copyOf(copied);
  }

  private static String decode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  private static void writeResponse(HttpExchange exchange, ChannelWebhookResponse response) throws IOException {
    byte[] body = response.body() == null ? new byte[0] : response.body().getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", response.contentType());
    for (Map.Entry<String, String> entry : response.headers().entrySet()) {
      exchange.getResponseHeaders().set(entry.getKey(), entry.getValue());
    }
    exchange.sendResponseHeaders(response.statusCode(), body.length);
    exchange.getResponseBody().write(body);
  }

  record ResolvedRoute(ChannelType channelType, String accountId) {
  }
}
