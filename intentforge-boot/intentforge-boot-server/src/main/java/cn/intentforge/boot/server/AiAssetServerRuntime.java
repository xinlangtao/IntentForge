package cn.intentforge.boot.server;

import cn.intentforge.boot.local.AiAssetLocalRuntime;
import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Running minimal HTTP server runtime for the native coding agent MVP.
 *
 * @param localRuntime underlying local runtime used by the server
 * @param httpServer running JDK HTTP server instance
 * @param baseUri externally consumable base URI
 * @param requestExecutor request executor configured for the server
 */
public record AiAssetServerRuntime(
    AiAssetLocalRuntime localRuntime,
    HttpServer httpServer,
    URI baseUri,
    ExecutorService requestExecutor
) implements AutoCloseable {
  /**
   * Creates a validated server runtime.
   */
  public AiAssetServerRuntime {
    localRuntime = Objects.requireNonNull(localRuntime, "localRuntime must not be null");
    httpServer = Objects.requireNonNull(httpServer, "httpServer must not be null");
    baseUri = Objects.requireNonNull(baseUri, "baseUri must not be null");
    requestExecutor = Objects.requireNonNull(requestExecutor, "requestExecutor must not be null");
  }

  /**
   * Stops the HTTP server and closes the request executor.
   */
  @Override
  public void close() {
    httpServer.stop(0);
    requestExecutor.close();
  }
}
