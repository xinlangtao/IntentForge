package cn.intentforge.boot.server;

import cn.intentforge.boot.local.AiAssetLocalBootstrap;
import cn.intentforge.boot.local.AiAssetLocalRuntime;
import cn.intentforge.space.SpaceRegistry;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Bootstraps the minimal HTTP and SSE server on top of the local runtime.
 */
public final class AiAssetServerBootstrap {
  /**
   * Default bind host used by the minimal server.
   */
  public static final String DEFAULT_HOST = "127.0.0.1";
  /**
   * Default bind port used by the minimal server.
   */
  public static final int DEFAULT_PORT = 8080;

  private AiAssetServerBootstrap() {
  }

  /**
   * Bootstraps and starts the server with runtime customization hooks.
   *
   * @param bindAddress target bind address
   * @param pluginsDirectory plugin directory used by the local runtime
   * @param spaceConfigurer callback used to register initial spaces
   * @param runtimeConfigurer callback used to seed runtime state before the server starts
   * @return running server runtime
   * @throws IOException when the JDK HTTP server cannot be created
   */
  public static AiAssetServerRuntime bootstrap(
      InetSocketAddress bindAddress,
      Path pluginsDirectory,
      Consumer<SpaceRegistry> spaceConfigurer,
      Consumer<AiAssetLocalRuntime> runtimeConfigurer
  ) throws IOException {
    InetSocketAddress nonNullBindAddress = Objects.requireNonNull(bindAddress, "bindAddress must not be null");
    Path nonNullPluginsDirectory = Objects.requireNonNull(pluginsDirectory, "pluginsDirectory must not be null");
    Consumer<SpaceRegistry> nonNullSpaceConfigurer = spaceConfigurer == null ? registry -> {
    } : spaceConfigurer;
    Consumer<AiAssetLocalRuntime> nonNullRuntimeConfigurer = runtimeConfigurer == null ? runtime -> {
    } : runtimeConfigurer;

    AiAssetLocalRuntime localRuntime = AiAssetLocalBootstrap.bootstrap(nonNullPluginsDirectory, nonNullSpaceConfigurer);
    nonNullRuntimeConfigurer.accept(localRuntime);

    ExecutorService requestExecutor = Executors.newVirtualThreadPerTaskExecutor();
    HttpServer server = HttpServer.create(nonNullBindAddress, 0);
    server.setExecutor(requestExecutor);

    AgentRunEventBroker eventBroker = new AgentRunEventBroker();
    AgentRunHttpApi httpApi = new AgentRunHttpApi(
        localRuntime.agentRunGateway(),
        localRuntime.sessionManager(),
        eventBroker);
    server.createContext("/api/agent-runs", httpApi::handleRunsRoot);
    server.createContext("/api/agent-runs/", httpApi::handleRunResource);
    server.start();

    return new AiAssetServerRuntime(localRuntime, server, baseUri(server), requestExecutor);
  }

  /**
   * Bootstraps and starts the server with default host and port.
   *
   * @param pluginsDirectory plugin directory used by the local runtime
   * @param spaceConfigurer callback used to register initial spaces
   * @param runtimeConfigurer callback used to seed runtime state before the server starts
   * @return running server runtime
   * @throws IOException when the JDK HTTP server cannot be created
   */
  public static AiAssetServerRuntime bootstrap(
      Path pluginsDirectory,
      Consumer<SpaceRegistry> spaceConfigurer,
      Consumer<AiAssetLocalRuntime> runtimeConfigurer
  ) throws IOException {
    return bootstrap(new InetSocketAddress(DEFAULT_HOST, DEFAULT_PORT), pluginsDirectory, spaceConfigurer, runtimeConfigurer);
  }

  private static URI baseUri(HttpServer server) {
    InetSocketAddress boundAddress = server.getAddress();
    InetAddress inetAddress = boundAddress.getAddress();
    String host = inetAddress == null || inetAddress.isAnyLocalAddress()
        ? DEFAULT_HOST
        : boundAddress.getHostString();
    return URI.create("http://" + host + ":" + boundAddress.getPort());
  }
}
