package cn.intentforge.hook;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Objects;

/**
 * JDK {@code HttpExchange} adapter for WeCom-specific hook endpoints.
 *
 * @since 1.0.0
 */
public final class WeComWebhookHttpExchangeHandler {
  /**
   * Shared path prefix for WeCom callback endpoints.
   */
  public static final String WECOM_CALLBACK_PREFIX = "/open-api/hooks/wecom/accounts/";

  private final ChannelWebhookEndpointController controller;

  /**
   * Creates one handler backed by the provided transport-neutral controller.
   *
   * @param controller transport-neutral webhook controller
   */
  public WeComWebhookHttpExchangeHandler(ChannelWebhookEndpointController controller) {
    this.controller = Objects.requireNonNull(controller, "controller must not be null");
  }

  /**
   * Handles one WeCom callback HTTP exchange routed under the WeCom-specific hook prefix.
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
          || !"wecom".equals(segments[3])
          || !"accounts".equals(segments[4])
          || !"callback".equals(segments[6])) {
        throw new HookEndpointException(404, "hook path not found");
      }
      String accountId = segments[5];
      if (accountId.isBlank()) {
        throw new HookEndpointException(404, "hook path not found");
      }
      return new Route(accountId);
    }

    private HookHttpExchangeSupport.ResolvedRoute resolvedRoute() {
      return HookHttpExchangeSupport.resolvedRoute(cn.intentforge.channel.ChannelType.WECOM, accountId);
    }
  }
}
