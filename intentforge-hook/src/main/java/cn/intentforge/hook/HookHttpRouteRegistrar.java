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
   * Registers the generic and platform-specific hook route families with one shared controller.
   *
   * @param server target JDK HTTP server
   * @param controller transport-neutral webhook controller shared by all hook paths
   */
  public static void register(HttpServer server, ChannelWebhookEndpointController controller) {
    Objects.requireNonNull(controller, "controller must not be null");
    register(server, new ChannelWebhookHttpExchangeHandler(controller));
    register(server, new TelegramWebhookHttpExchangeHandler(controller));
    register(server, new WeComWebhookHttpExchangeHandler(controller));
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

  /**
   * Registers the Telegram-specific webhook route prefix.
   *
   * @param server target JDK HTTP server
   * @param handler Telegram hook exchange handler
   */
  public static void register(HttpServer server, TelegramWebhookHttpExchangeHandler handler) {
    Objects.requireNonNull(server, "server must not be null");
    TelegramWebhookHttpExchangeHandler nonNullHandler = Objects.requireNonNull(handler, "handler must not be null");
    server.createContext(TelegramWebhookHttpExchangeHandler.TELEGRAM_WEBHOOK_PREFIX, nonNullHandler::handle);
  }

  /**
   * Registers the WeCom-specific callback route prefix.
   *
   * @param server target JDK HTTP server
   * @param handler WeCom hook exchange handler
   */
  public static void register(HttpServer server, WeComWebhookHttpExchangeHandler handler) {
    Objects.requireNonNull(server, "server must not be null");
    WeComWebhookHttpExchangeHandler nonNullHandler = Objects.requireNonNull(handler, "handler must not be null");
    server.createContext(WeComWebhookHttpExchangeHandler.WECOM_CALLBACK_PREFIX, nonNullHandler::handle);
  }
}
