package cn.intentforge.hook;

import com.sun.net.httpserver.HttpServer;
import java.util.Objects;

/**
 * Registers externally visible hook HTTP contexts on the minimal JDK server.
 *
 * @since 1.0.0
 */
public final class HookHttpRouteRegistrar {
  private HookHttpRouteRegistrar() {
  }

  /**
   * Registers the generic channel webhook route prefix.
   *
   * @param server target JDK HTTP server
   * @param handler hook exchange handler
   */
  public static void register(HttpServer server, ChannelWebhookHttpExchangeHandler handler) {
    Objects.requireNonNull(server, "server must not be null");
    ChannelWebhookHttpExchangeHandler nonNullHandler = Objects.requireNonNull(handler, "handler must not be null");
    server.createContext(ChannelWebhookHttpExchangeHandler.CHANNEL_WEBHOOK_PREFIX, nonNullHandler::handle);
  }
}
