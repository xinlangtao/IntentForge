package cn.intentforge.boot.server;

import cn.intentforge.boot.local.AiAssetLocalRuntime;
import cn.intentforge.model.catalog.ModelCapability;
import cn.intentforge.model.catalog.ModelDescriptor;
import cn.intentforge.model.catalog.ModelType;
import cn.intentforge.model.provider.ModelProvider;
import cn.intentforge.model.provider.ModelProviderDescriptor;
import cn.intentforge.model.provider.ModelProviderType;
import cn.intentforge.prompt.model.PromptDefinition;
import cn.intentforge.prompt.model.PromptKind;
import cn.intentforge.session.model.SessionDraft;
import cn.intentforge.space.SpaceDefinition;
import cn.intentforge.space.SpaceProfile;
import cn.intentforge.space.SpaceRegistry;
import cn.intentforge.space.SpaceType;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Minimal terminal entrypoint that starts the demo HTTP/SSE server for local smoke testing.
 */
public final class AiAssetServerMain {
  private AiAssetServerMain() {
  }

  /**
   * Starts the minimal demo server and blocks the main thread until the process is terminated.
   *
   * <p>Supported system properties:
   * <ul>
   *   <li>{@code intentforge.server.host}</li>
   *   <li>{@code intentforge.server.port}</li>
   *   <li>{@code intentforge.pluginsDir}</li>
   * </ul>
   *
   * @param args unused CLI arguments
   * @throws Exception when startup fails
   */
  public static void main(String[] args) throws Exception {
    String host = System.getProperty("intentforge.server.host", AiAssetServerBootstrap.DEFAULT_HOST);
    int port = Integer.getInteger("intentforge.server.port", AiAssetServerBootstrap.DEFAULT_PORT);
    Path pluginsDirectory = Path.of(System.getProperty("intentforge.pluginsDir", "plugins"));

    AiAssetServerRuntime runtime = AiAssetServerBootstrap.bootstrap(
        new InetSocketAddress(host, port),
        pluginsDirectory,
        AiAssetServerMain::registerDemoSpaces,
        AiAssetServerMain::seedDemoRuntime);

    Runtime.getRuntime().addShutdownHook(new Thread(runtime::close));

    System.out.println("IntentForge demo server started at: " + runtime.baseUri());
    System.out.println("demo sessionId: session-1");
    System.out.println("create run endpoint: " + runtime.baseUri().resolve("/api/agent-runs"));
    System.out.println("request handling prefers virtual threads: true");

    new CountDownLatch(1).await();
  }

  private static void registerDemoSpaces(SpaceRegistry registry) {
    registry.registerAll(List.of(
        new SpaceDefinition("company-root", SpaceType.COMPANY, null, SpaceProfile.empty()),
        new SpaceDefinition("project-alpha", SpaceType.PROJECT, "company-root", SpaceProfile.empty()),
        new SpaceDefinition("product-alpha", SpaceType.PRODUCT, "project-alpha", SpaceProfile.empty()),
        new SpaceDefinition("application-alpha", SpaceType.APPLICATION, "product-alpha", new SpaceProfile(
            List.of(),
            List.of("intentforge.native.planner", "intentforge.native.coder", "intentforge.native.reviewer"),
            List.of("prompt-1"),
            List.of("intentforge.fs.list", "intentforge.runtime.environment.read"),
            List.of("model-1"),
            List.of("provider-1"),
            List.of(),
            Map.of("review.level", "mvp")))));
  }

  private static void seedDemoRuntime(AiAssetLocalRuntime runtime) {
    runtime.promptManager().register(new PromptDefinition(
        "prompt-1",
        "v1",
        "Planner Prompt",
        "prompt",
        PromptKind.SYSTEM,
        List.of(),
        List.of("coding"),
        "Plan the task",
        Map.of()));
    runtime.modelManager().register(new ModelDescriptor(
        "model-1",
        "provider-1",
        "Coding Model",
        "model",
        ModelType.CHAT,
        List.of(ModelCapability.CHAT, ModelCapability.REASONING),
        32000,
        true,
        Map.of()));
    runtime.providerRegistry().register(new ModelProvider() {
      @Override
      public ModelProviderDescriptor descriptor() {
        return new ModelProviderDescriptor(
            "provider-1",
            "Provider",
            "provider",
            ModelProviderType.CUSTOM,
            "native://provider",
            List.of(ModelCapability.CHAT),
            Map.of());
      }
    });
    runtime.sessionManager().create(new SessionDraft("session-1", "Coding", "application-alpha", Map.of()));
  }
}
