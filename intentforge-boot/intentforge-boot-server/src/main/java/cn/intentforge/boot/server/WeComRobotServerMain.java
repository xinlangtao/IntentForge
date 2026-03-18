package cn.intentforge.boot.server;

import cn.intentforge.hook.HookEndpointPaths;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

/**
 * Terminal entrypoint that starts the minimal server with one manually registered WeCom intelligent-robot hook account.
 *
 * <p>Supported system properties:
 * <ul>
 *   <li>{@code intentforge.server.host}</li>
 *   <li>{@code intentforge.server.port}</li>
 *   <li>{@code intentforge.pluginsDir}</li>
 *   <li>{@code intentforge.wecom.accountId}</li>
 *   <li>{@code intentforge.wecom.displayName}</li>
 *   <li>{@code intentforge.wecom.callbackToken}</li>
 *   <li>{@code intentforge.wecom.callbackEncodingAesKey}</li>
 *   <li>{@code intentforge.wecom.receiveId}</li>
 *   <li>{@code intentforge.wecom.robotId}</li>
 *   <li>{@code intentforge.wecom.robotSecret}</li>
 *   <li>{@code intentforge.wecom.baseUrl}</li>
 * </ul>
 *
 * <p>Supported environment variables:
 * <ul>
 *   <li>{@code WECOM_ACCOUNT_ID}</li>
 *   <li>{@code WECOM_DISPLAY_NAME}</li>
 *   <li>{@code WECOM_CALLBACK_TOKEN}</li>
 *   <li>{@code WECOM_CALLBACK_AES_KEY}</li>
 *   <li>{@code WECOM_RECEIVE_ID}</li>
 *   <li>{@code WECOM_ROBOT_ID}</li>
 *   <li>{@code WECOM_ROBOT_SECRET}</li>
 *   <li>{@code WECOM_BASE_URL}</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class WeComRobotServerMain {
  private WeComRobotServerMain() {
  }

  /**
   * Starts the WeCom intelligent-robot local server and blocks the main thread until the process is terminated.
   *
   * @param args unused CLI arguments
   * @throws Exception when startup fails
   */
  public static void main(String[] args) throws Exception {
    String host = System.getProperty("intentforge.server.host", AiAssetServerBootstrap.DEFAULT_HOST);
    int port = Integer.getInteger("intentforge.server.port", AiAssetServerBootstrap.DEFAULT_PORT);
    Path pluginsDirectory = Path.of(System.getProperty("intentforge.pluginsDir", "plugins"));
    WeComRobotServerSettings settings = resolveSettings(System::getProperty, System::getenv);

    AiAssetServerRuntime runtime = startServer(new InetSocketAddress(host, port), pluginsDirectory, settings);
    Runtime.getRuntime().addShutdownHook(new Thread(runtime::close));

    System.out.println("IntentForge WeCom robot server started at: " + runtime.baseUri());
    System.out.println("wecom callback endpoint: "
        + runtime.baseUri().resolve(HookEndpointPaths.canonicalPath(settings.toAccountProfile().type(), settings.accountId())));
    System.out.println("request handling prefers virtual threads: true");

    new CountDownLatch(1).await();
  }

  static AiAssetServerRuntime startServer(
      InetSocketAddress bindAddress,
      Path pluginsDirectory,
      WeComRobotServerSettings settings
  ) throws IOException {
    Objects.requireNonNull(settings, "settings must not be null");
    return AiAssetServerBootstrap.bootstrap(
        bindAddress,
        pluginsDirectory,
        null,
        null,
        hookAccounts -> hookAccounts.register(settings.toAccountProfile()));
  }

  static WeComRobotServerSettings resolveSettings(
      Function<String, String> systemPropertyLookup,
      Function<String, String> environmentLookup
  ) {
    Function<String, String> nonNullSystemPropertyLookup =
        systemPropertyLookup == null ? key -> null : systemPropertyLookup;
    Function<String, String> nonNullEnvironmentLookup =
        environmentLookup == null ? key -> null : environmentLookup;

    String accountId = firstNonBlank(
        nonNullSystemPropertyLookup.apply("intentforge.wecom.accountId"),
        nonNullEnvironmentLookup.apply("WECOM_ACCOUNT_ID"),
        "wecom-account");
    String displayName = firstNonBlank(
        nonNullSystemPropertyLookup.apply("intentforge.wecom.displayName"),
        nonNullEnvironmentLookup.apply("WECOM_DISPLAY_NAME"),
        "WeCom Robot");
    String callbackToken = requiredText(
        firstNonBlank(
            nonNullSystemPropertyLookup.apply("intentforge.wecom.callbackToken"),
            nonNullEnvironmentLookup.apply("WECOM_CALLBACK_TOKEN"),
            null),
        "wecom callback token");
    String callbackEncodingAesKey = requiredText(
        firstNonBlank(
            nonNullSystemPropertyLookup.apply("intentforge.wecom.callbackEncodingAesKey"),
            firstNonBlank(
                nonNullEnvironmentLookup.apply("WECOM_CALLBACK_ENCODING_AES_KEY"),
                nonNullEnvironmentLookup.apply("WECOM_CALLBACK_AES_KEY"),
                null),
            null),
        "wecom callback AES key");
    String receiveId = firstNonBlank(
        nonNullSystemPropertyLookup.apply("intentforge.wecom.receiveId"),
        nonNullEnvironmentLookup.apply("WECOM_RECEIVE_ID"),
        null);
    String robotId = requiredText(
        firstNonBlank(
            nonNullSystemPropertyLookup.apply("intentforge.wecom.robotId"),
            nonNullEnvironmentLookup.apply("WECOM_ROBOT_ID"),
            null),
        "wecom robot id");
    String robotSecret = requiredText(
        firstNonBlank(
            nonNullSystemPropertyLookup.apply("intentforge.wecom.robotSecret"),
            nonNullEnvironmentLookup.apply("WECOM_ROBOT_SECRET"),
            null),
        "wecom robot secret");
    String baseUrl = firstNonBlank(
        nonNullSystemPropertyLookup.apply("intentforge.wecom.baseUrl"),
        nonNullEnvironmentLookup.apply("WECOM_BASE_URL"),
        null);

    return new WeComRobotServerSettings(
        accountId,
        displayName,
        callbackToken,
        callbackEncodingAesKey,
        receiveId,
        robotId,
        robotSecret,
        baseUrl);
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
}
