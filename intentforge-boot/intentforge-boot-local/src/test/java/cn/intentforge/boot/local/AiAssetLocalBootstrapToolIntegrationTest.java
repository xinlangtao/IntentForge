package cn.intentforge.boot.local;

import cn.intentforge.space.ResolvedSpaceProfile;
import cn.intentforge.space.SpaceDefinition;
import cn.intentforge.space.SpaceProfile;
import cn.intentforge.space.SpaceType;
import cn.intentforge.tool.core.model.ToolCallRequest;
import cn.intentforge.tool.core.model.ToolCallStatus;
import cn.intentforge.tool.core.model.ToolExecutionContext;
import cn.intentforge.tool.core.permission.DefaultToolPermissionPolicy;
import cn.intentforge.tool.connectors.ConnectorToolPlugin;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AiAssetLocalBootstrapToolIntegrationTest {
  @Test
  void shouldBootstrapAndExecuteShellFsWebTools() throws Exception {
    Path workspace = Files.createTempDirectory("boot-tool-workspace");
    Path pluginsDirectory = Files.createTempDirectory("boot-tool-plugins");
    AiAssetLocalRuntime runtime = AiAssetLocalBootstrap.bootstrap(pluginsDirectory);

    Assertions.assertTrue(runtime.toolRegistry().find("intentforge.shell.exec").isPresent());
    Assertions.assertTrue(runtime.toolRegistry().find("intentforge.fs.list").isPresent());
    Assertions.assertTrue(runtime.toolRegistry().find("intentforge.web.fetch").isPresent());
    Assertions.assertTrue(runtime.toolRegistry().find(ConnectorToolPlugin.TOOL_RUNTIME_ENVIRONMENT_READ).isPresent());

    if (runtime.toolPermissionPolicy() instanceof DefaultToolPermissionPolicy policy) {
      policy.allow("intentforge.shell.exec");
      policy.allow("intentforge.web.fetch");
    }

    ToolExecutionContext context = ToolExecutionContext.create(workspace);

    var shellResult = runtime.toolGateway().execute(new ToolCallRequest(
        "intentforge.shell.exec",
        Map.of("command", "echo boot-ok"),
        context));
    Assertions.assertEquals(ToolCallStatus.SUCCESS, shellResult.status());
    Assertions.assertTrue(shellResult.output().contains("boot-ok"));

    var fsResult = runtime.toolGateway().execute(new ToolCallRequest(
        "intentforge.fs.list",
        Map.of("path", "."),
        context));
    Assertions.assertEquals(ToolCallStatus.SUCCESS, fsResult.status());

    var environmentResult = runtime.toolGateway().execute(new ToolCallRequest(
        ConnectorToolPlugin.TOOL_RUNTIME_ENVIRONMENT_READ,
        Map.of(),
        context));
    Assertions.assertEquals(ToolCallStatus.SUCCESS, environmentResult.status());
    Assertions.assertTrue(environmentResult.structured() instanceof Map<?, ?>);

    try (TestServer server = TestServer.start()) {
      var webResult = runtime.toolGateway().execute(new ToolCallRequest(
          "intentforge.web.fetch",
          Map.of("url", server.uri("/hello").toString()),
          context));
      Assertions.assertEquals(ToolCallStatus.SUCCESS, webResult.status());
      Assertions.assertTrue(webResult.output().contains("hello"));
    }
  }

  @Test
  void shouldBootstrapWithSpaceRegistryAndResolver() throws Exception {
    Path pluginsDirectory = Files.createTempDirectory("boot-space-plugins");
    AiAssetLocalRuntime runtime = AiAssetLocalBootstrap.bootstrap(pluginsDirectory, registry -> registry.registerAll(List.of(
        new SpaceDefinition("company-root", SpaceType.COMPANY, null, new SpaceProfile(
            List.of("skill-company"),
            List.of("agent-company"),
            List.of("prompt-company"),
            List.of("tool-company"),
            null,
            List.of("provider-company"),
            null,
            Map.of("timeout", "30"))),
        new SpaceDefinition("project-alpha", SpaceType.PROJECT, "company-root", new SpaceProfile(
            null,
            null,
            null,
            null,
            null,
            null,
            List.of("memory-project"),
            Map.of("region", "cn"))),
        new SpaceDefinition("product-alpha", SpaceType.PRODUCT, "project-alpha", new SpaceProfile(
            null,
            null,
            List.of("prompt-product"),
            null,
            List.of("model-product"),
            null,
            null,
            Map.of())),
        new SpaceDefinition("application-alpha", SpaceType.APPLICATION, "product-alpha", new SpaceProfile(
            List.of("skill-app"),
            List.of("agent-app"),
            null,
            null,
            null,
            null,
            null,
            Map.of("timeout", "10"))))));

    Assertions.assertTrue(runtime.spaceRegistry().find("company-root").isPresent());

    ResolvedSpaceProfile resolved = runtime.spaceResolver().resolve("application-alpha");
    Assertions.assertEquals(List.of("company-root", "project-alpha", "product-alpha", "application-alpha"),
        resolved.inheritancePath());
    Assertions.assertEquals(List.of("skill-app"), resolved.skillIds());
    Assertions.assertEquals(List.of("agent-app"), resolved.agentIds());
    Assertions.assertEquals(List.of("prompt-product"), resolved.promptIds());
    Assertions.assertEquals(List.of("model-product"), resolved.modelIds());
    Assertions.assertEquals(List.of("provider-company"), resolved.modelProviderIds());
    Assertions.assertEquals(List.of("memory-project"), resolved.memoryIds());
    Assertions.assertEquals(Map.of("timeout", "10", "region", "cn"), resolved.config());
  }

  private static final class TestServer implements AutoCloseable {
    private final HttpServer server;

    private TestServer(HttpServer server) {
      this.server = server;
    }

    static TestServer start() throws Exception {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/hello", exchange -> {
        byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
      });
      server.start();
      return new TestServer(server);
    }

    URI uri(String path) {
      return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    @Override
    public void close() {
      server.stop(0);
    }
  }
}
