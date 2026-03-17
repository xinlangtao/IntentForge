package cn.intentforge.hook;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Objects;

/**
 * JDK {@code HttpExchange} adapter for Telegram-specific hook endpoints.
 *
 * @since 1.0.0
 */
public final class TelegramWebhookHttpExchangeHandler {
  /**
   * Shared path prefix for Telegram webhook endpoints.
   */
  public static final String TELEGRAM_WEBHOOK_PREFIX = "/open-api/hooks/telegram/accounts/";

  private final ChannelWebhookEndpointController controller;

  /**
   * Creates one handler backed by the provided transport-neutral controller.
   *
   * @param controller transport-neutral webhook controller
   */
  public TelegramWebhookHttpExchangeHandler(ChannelWebhookEndpointController controller) {
    this.controller = Objects.requireNonNull(controller, "controller must not be null");
  }

  /**
   * Handles one Telegram webhook HTTP exchange routed under the Telegram-specific hook prefix.
   *
   * @param exchange incoming HTTP exchange
   * @throws IOException when the request or response body cannot be processed
   */
  public void handle(HttpExchange exchange) throws IOException {
    HookHttpExchangeSupport.handle(exchange, controller, path -> Route.parse(path).resolvedRoute());
  }

  private record Route(String accountId) {
    private static Route parse(String path) {
      String[] segments = path == null ? new String[0] : path.split("/");
      if (segments.length != 7
          || !"open-api".equals(segments[1])
          || !"hooks".equals(segments[2])
          || !"telegram".equals(segments[3])
          || !"accounts".equals(segments[4])
          || !"webhook".equals(segments[6])) {
        throw new HookEndpointException(404, "hook path not found");
      }
      String accountId = segments[5];
      if (accountId.isBlank()) {
        throw new HookEndpointException(404, "hook path not found");
      }
      return new Route(accountId);
    }

    private HookHttpExchangeSupport.ResolvedRoute resolvedRoute() {
      return HookHttpExchangeSupport.resolvedRoute(cn.intentforge.channel.ChannelType.TELEGRAM, accountId);
    }
  }
}
