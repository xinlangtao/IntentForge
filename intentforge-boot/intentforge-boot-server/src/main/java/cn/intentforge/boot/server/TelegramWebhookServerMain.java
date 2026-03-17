package cn.intentforge.boot.server;

import cn.intentforge.hook.HookEndpointPaths;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

/**
 * Terminal entrypoint that starts the minimal server with one manually registered Telegram hook account.
 *
 * <p>Supported system properties:
 * <ul>
 *   <li>{@code intentforge.server.host}</li>
 *   <li>{@code intentforge.server.port}</li>
 *   <li>{@code intentforge.pluginsDir}</li>
 *   <li>{@code intentforge.telegram.accountId}</li>
 *   <li>{@code intentforge.telegram.displayName}</li>
 *   <li>{@code intentforge.telegram.botToken}</li>
 *   <li>{@code intentforge.telegram.baseUrl}</li>
 *   <li>{@code intentforge.telegram.webhookUrl}</li>
 *   <li>{@code intentforge.telegram.webhookBaseUrl}</li>
 *   <li>{@code intentforge.telegram.webhookSecretToken}</li>
 *   <li>{@code intentforge.telegram.webhookAllowedUpdates}</li>
 *   <li>{@code intentforge.telegram.webhookMaxConnections}</li>
 *   <li>{@code intentforge.telegram.webhookDropPendingUpdates}</li>
 *   <li>{@code intentforge.telegram.webhookAutoManage}</li>
 * </ul>
 *
 * <p>Supported environment variables:
 * <ul>
 *   <li>{@code TG_ACCOUNT_ID}</li>
 *   <li>{@code TG_DISPLAY_NAME}</li>
 *   <li>{@code TG_BOT_TOKEN}</li>
 *   <li>{@code TG_BASE_URL}</li>
 *   <li>{@code TG_WEBHOOK_URL}</li>
 *   <li>{@code TG_WEBHOOK_BASE_URL}</li>
 *   <li>{@code TG_WEBHOOK_SECRET}</li>
 *   <li>{@code TG_WEBHOOK_ALLOWED_UPDATES}</li>
 *   <li>{@code TG_WEBHOOK_MAX_CONNECTIONS}</li>
 *   <li>{@code TG_WEBHOOK_DROP_PENDING_UPDATES}</li>
 *   <li>{@code TG_WEBHOOK_AUTO_MANAGE}</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class TelegramWebhookServerMain {
  private TelegramWebhookServerMain() {
  }

  /**
   * Starts the Telegram-focused local server and blocks the main thread until the process is terminated.
   *
   * @param args unused CLI arguments
   * @throws Exception when startup fails
   */
  public static void main(String[] args) throws Exception {
    String host = System.getProperty("intentforge.server.host", AiAssetServerBootstrap.DEFAULT_HOST);
    int port = Integer.getInteger("intentforge.server.port", AiAssetServerBootstrap.DEFAULT_PORT);
    Path pluginsDirectory = Path.of(System.getProperty("intentforge.pluginsDir", "plugins"));
    TelegramWebhookServerSettings settings = resolveSettings(System::getProperty, System::getenv);

    AiAssetServerRuntime runtime = startServer(new InetSocketAddress(host, port), pluginsDirectory, settings);
    Runtime.getRuntime().addShutdownHook(new Thread(runtime::close));

    System.out.println("IntentForge Telegram server started at: " + runtime.baseUri());
    System.out.println("telegram webhook endpoint: " + runtime.baseUri().resolve(HookEndpointPaths.canonicalPath(settings.toAccountProfile().type(), settings.accountId())));
    System.out.println("telegram webhook auto-manage enabled: " + settings.webhookAutoManage());
    System.out.println("request handling prefers virtual threads: true");

    new CountDownLatch(1).await();
  }

  static AiAssetServerRuntime startServer(
      InetSocketAddress bindAddress,
      Path pluginsDirectory,
      TelegramWebhookServerSettings settings
  ) throws IOException {
    Objects.requireNonNull(settings, "settings must not be null");
    return AiAssetServerBootstrap.bootstrap(
        bindAddress,
        pluginsDirectory,
        null,
        null,
        hookAccounts -> hookAccounts.register(settings.toAccountProfile()));
  }

  static TelegramWebhookServerSettings resolveSettings(
      Function<String, String> systemPropertyLookup,
      Function<String, String> environmentLookup
  ) {
    Function<String, String> nonNullSystemPropertyLookup =
        systemPropertyLookup == null ? key -> null : systemPropertyLookup;
    Function<String, String> nonNullEnvironmentLookup =
        environmentLookup == null ? key -> null : environmentLookup;

    String accountId = firstNonBlank(
        nonNullSystemPropertyLookup.apply("intentforge.telegram.accountId"),
        nonNullEnvironmentLookup.apply("TG_ACCOUNT_ID"),
        "telegram-account");
    String displayName = firstNonBlank(
        nonNullSystemPropertyLookup.apply("intentforge.telegram.displayName"),
        nonNullEnvironmentLookup.apply("TG_DISPLAY_NAME"),
        "Telegram Bot");
    String botToken = requiredText(
        firstNonBlank(
            nonNullSystemPropertyLookup.apply("intentforge.telegram.botToken"),
            nonNullEnvironmentLookup.apply("TG_BOT_TOKEN"),
            null),
        "telegram bot token");
    String baseUrl = firstNonBlank(
        nonNullSystemPropertyLookup.apply("intentforge.telegram.baseUrl"),
        nonNullEnvironmentLookup.apply("TG_BASE_URL"),
        null);
    String webhookUrl = firstNonBlank(
        nonNullSystemPropertyLookup.apply("intentforge.telegram.webhookUrl"),
        nonNullEnvironmentLookup.apply("TG_WEBHOOK_URL"),
        null);
    String webhookBaseUrl = firstNonBlank(
        nonNullSystemPropertyLookup.apply("intentforge.telegram.webhookBaseUrl"),
        nonNullEnvironmentLookup.apply("TG_WEBHOOK_BASE_URL"),
        null);
    String webhookSecretToken = firstNonBlank(
        nonNullSystemPropertyLookup.apply("intentforge.telegram.webhookSecretToken"),
        nonNullEnvironmentLookup.apply("TG_WEBHOOK_SECRET"),
        null);
    String webhookAllowedUpdates = firstNonBlank(
        nonNullSystemPropertyLookup.apply("intentforge.telegram.webhookAllowedUpdates"),
        nonNullEnvironmentLookup.apply("TG_WEBHOOK_ALLOWED_UPDATES"),
        null);
    Integer webhookMaxConnections = parseInteger(
        firstNonBlank(
            nonNullSystemPropertyLookup.apply("intentforge.telegram.webhookMaxConnections"),
            nonNullEnvironmentLookup.apply("TG_WEBHOOK_MAX_CONNECTIONS"),
            null),
        "telegram webhook max connections");
    boolean webhookDropPendingUpdates = parseBoolean(
        firstNonBlank(
            nonNullSystemPropertyLookup.apply("intentforge.telegram.webhookDropPendingUpdates"),
            nonNullEnvironmentLookup.apply("TG_WEBHOOK_DROP_PENDING_UPDATES"),
            null),
        false);
    String rawAutoManage = firstNonBlank(
        nonNullSystemPropertyLookup.apply("intentforge.telegram.webhookAutoManage"),
        nonNullEnvironmentLookup.apply("TG_WEBHOOK_AUTO_MANAGE"),
        null);
    boolean webhookAutoManage = rawAutoManage == null
        ? webhookUrl != null || webhookBaseUrl != null
        : parseBoolean(rawAutoManage, false);

    return new TelegramWebhookServerSettings(
        accountId,
        displayName,
        botToken,
        baseUrl,
        webhookUrl,
        webhookBaseUrl,
        webhookSecretToken,
        webhookAllowedUpdates,
        webhookMaxConnections,
        webhookDropPendingUpdates,
        webhookAutoManage);
  }

  private static String firstNonBlank(String preferred, String fallback, String defaultValue) {
    String normalizedPreferred = normalize(preferred);
    if (normalizedPreferred != null) {
      return normalizedPreferred;
    }
    String normalizedFallback = normalize(fallback);
    return normalizedFallback == null ? normalize(defaultValue) : normalizedFallback;
  }

  private static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String requiredText(String value, String fieldName) {
    String normalized = normalize(value);
    if (normalized == null) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }

  private static Integer parseInteger(String value, String fieldName) {
    String normalized = normalize(value);
    if (normalized == null) {
      return null;
    }
    try {
      return Integer.valueOf(normalized);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(fieldName + " must be a valid integer", exception);
    }
  }

  private static boolean parseBoolean(String value, boolean defaultValue) {
    String normalized = normalize(value);
    return normalized == null ? defaultValue : Boolean.parseBoolean(normalized);
  }
}
