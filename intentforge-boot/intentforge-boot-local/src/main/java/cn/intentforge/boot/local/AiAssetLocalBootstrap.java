package cn.intentforge.boot.local;

import cn.intentforge.model.local.plugin.DirectoryModelPluginManager;
import cn.intentforge.model.local.registry.InMemoryModelManager;
import cn.intentforge.model.provider.local.plugin.DirectoryModelProviderPluginManager;
import cn.intentforge.model.provider.local.registry.InMemoryModelProviderRegistry;
import cn.intentforge.model.provider.registry.ModelProviderRegistry;
import cn.intentforge.model.provider.spi.ModelProviderRegistryProvider;
import cn.intentforge.model.registry.ModelManager;
import cn.intentforge.model.spi.ModelManagerProvider;
import cn.intentforge.prompt.local.plugin.DirectoryPromptPluginManager;
import cn.intentforge.prompt.local.registry.InMemoryPromptManager;
import cn.intentforge.prompt.registry.PromptManager;
import cn.intentforge.prompt.spi.PromptManagerProvider;
import cn.intentforge.session.local.SessionLocalRuntime;
import cn.intentforge.session.local.SessionLocalRuntimeFactory;
import cn.intentforge.session.registry.SessionManager;
import cn.intentforge.space.SpaceRegistry;
import cn.intentforge.space.SpaceResolver;
import cn.intentforge.space.local.SpaceLocalRuntime;
import cn.intentforge.space.local.SpaceLocalRuntimeFactory;
import cn.intentforge.tool.core.gateway.DefaultToolGateway;
import cn.intentforge.tool.core.gateway.ToolGateway;
import cn.intentforge.tool.core.local.plugin.DirectoryToolPluginManager;
import cn.intentforge.tool.core.permission.DefaultToolPermissionPolicy;
import cn.intentforge.tool.core.permission.ToolPermissionPolicy;
import cn.intentforge.tool.core.registry.InMemoryToolRegistry;
import cn.intentforge.tool.core.registry.ToolRegistry;
import cn.intentforge.tool.core.spi.ToolRegistryProvider;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Bootstraps local runtime assets from classpath SPI and external plugin directories.
 */
public final class AiAssetLocalBootstrap {
  /**
   * Default plugin directory path.
   */
  public static final Path DEFAULT_PLUGIN_DIRECTORY = Path.of("plugins");
  private static final List<String> DEFAULT_SENSITIVE_TOOL_IDS = List.of(
      "intentforge.shell.exec",
      "intentforge.fs.write",
      "intentforge.fs.edit",
      "intentforge.fs.apply-patch",
      "intentforge.web.fetch",
      "intentforge.web.search",
      "intentforge.flow.task.dispatch");

  private AiAssetLocalBootstrap() {
  }

  /**
   * Bootstraps runtime with {@link #DEFAULT_PLUGIN_DIRECTORY}.
   *
   * @return local runtime wiring
   */
  public static AiAssetLocalRuntime bootstrap() {
    return bootstrap(DEFAULT_PLUGIN_DIRECTORY);
  }

  /**
   * Bootstraps runtime with the provided plugin directory.
   *
   * @param pluginsDirectory plugin directory
   * @return local runtime wiring
   */
  public static AiAssetLocalRuntime bootstrap(Path pluginsDirectory) {
    return bootstrap(pluginsDirectory, registry -> {
    });
  }

  /**
   * Bootstraps runtime with the provided plugin directory and initial space definitions.
   *
   * @param pluginsDirectory plugin directory
   * @param spaceConfigurer callback used to register initial space definitions
   * @return local runtime wiring
   */
  public static AiAssetLocalRuntime bootstrap(Path pluginsDirectory, Consumer<SpaceRegistry> spaceConfigurer) {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    SpaceLocalRuntime spaceLocalRuntime = SpaceLocalRuntimeFactory.create(spaceConfigurer);
    SpaceRegistry spaceRegistry = spaceLocalRuntime.spaceRegistry();
    SpaceResolver spaceResolver = spaceLocalRuntime.spaceResolver();
    SessionLocalRuntime sessionLocalRuntime = SessionLocalRuntimeFactory.create(classLoader);
    SessionManager sessionManager = sessionLocalRuntime.sessionManager();

    PromptManager promptManager = createPromptManager(classLoader);
    promptManager.loadPlugins();
    DirectoryPromptPluginManager promptPluginManager =
        new DirectoryPromptPluginManager(pluginsDirectory, promptManager);
    promptPluginManager.loadAll();

    ModelManager modelManager = createModelManager(classLoader);
    modelManager.loadPlugins();
    DirectoryModelPluginManager modelPluginManager =
        new DirectoryModelPluginManager(pluginsDirectory, modelManager);
    modelPluginManager.loadAll();

    ModelProviderRegistry providerRegistry = createProviderRegistry(classLoader);
    providerRegistry.loadPlugins();
    DirectoryModelProviderPluginManager providerPluginManager =
        new DirectoryModelProviderPluginManager(pluginsDirectory, providerRegistry, modelManager);
    providerPluginManager.loadAll();

    ToolRegistry toolRegistry = createToolRegistry(classLoader);
    toolRegistry.loadPlugins();
    DirectoryToolPluginManager toolPluginManager =
        new DirectoryToolPluginManager(pluginsDirectory, toolRegistry);
    toolPluginManager.loadAll();

    DefaultToolPermissionPolicy toolPermissionPolicy = new DefaultToolPermissionPolicy(DEFAULT_SENSITIVE_TOOL_IDS);
    ToolGateway toolGateway = new DefaultToolGateway(toolRegistry, toolPermissionPolicy);

    return new AiAssetLocalRuntime(
        promptManager,
        promptPluginManager,
        modelManager,
        modelPluginManager,
        providerRegistry,
        providerPluginManager,
        toolRegistry,
        toolPluginManager,
        toolPermissionPolicy,
        toolGateway,
        sessionManager,
        spaceRegistry,
        spaceResolver);
  }

  private static PromptManager createPromptManager(ClassLoader classLoader) {
    return selectSingleProvider(
        classLoader,
        PromptManagerProvider.class,
        PromptManagerProvider::priority,
        provider -> provider.create(classLoader),
        () -> new InMemoryPromptManager(classLoader));
  }

  private static ModelManager createModelManager(ClassLoader classLoader) {
    return selectSingleProvider(
        classLoader,
        ModelManagerProvider.class,
        ModelManagerProvider::priority,
        provider -> provider.create(classLoader),
        () -> new InMemoryModelManager(classLoader));
  }

  private static ModelProviderRegistry createProviderRegistry(ClassLoader classLoader) {
    return selectSingleProvider(
        classLoader,
        ModelProviderRegistryProvider.class,
        ModelProviderRegistryProvider::priority,
        provider -> provider.create(classLoader),
        () -> new InMemoryModelProviderRegistry(classLoader));
  }

  private static ToolRegistry createToolRegistry(ClassLoader classLoader) {
    return selectSingleProvider(
        classLoader,
        ToolRegistryProvider.class,
        ToolRegistryProvider::priority,
        provider -> provider.create(classLoader),
        () -> new InMemoryToolRegistry(classLoader));
  }

  static <P, R> R selectSingleProvider(
      ClassLoader classLoader,
      Class<P> providerType,
      ToIntFunction<P> priorityExtractor,
      Function<P, R> instanceExtractor,
      Supplier<R> defaultSupplier
  ) {
    List<P> providers = ServiceLoader.load(providerType, classLoader)
        .stream()
        .map(ServiceLoader.Provider::get)
        .toList();
    return selectSingleProvider(providers, providerType, priorityExtractor, instanceExtractor, defaultSupplier);
  }

  static <P, R> R selectSingleProvider(
      List<P> providers,
      Class<P> providerType,
      ToIntFunction<P> priorityExtractor,
      Function<P, R> instanceExtractor,
      Supplier<R> defaultSupplier
  ) {
    if (providers.isEmpty()) {
      return defaultSupplier.get();
    }

    P winner = null;
    int winnerPriority = Integer.MIN_VALUE;
    List<P> candidates = new ArrayList<>();
    for (P provider : providers) {
      int priority = priorityExtractor.applyAsInt(provider);
      if (winner == null || priority > winnerPriority) {
        winner = provider;
        winnerPriority = priority;
        candidates.clear();
        candidates.add(provider);
        continue;
      }
      if (priority == winnerPriority) {
        candidates.add(provider);
      }
    }

    if (candidates.size() > 1) {
      String providerNames = candidates.stream()
          .map(provider -> provider.getClass().getName())
          .sorted()
          .reduce((left, right) -> left + ", " + right)
          .orElse("");
      throw new IllegalStateException(
          "multiple " + providerType.getSimpleName() + " with priority " + winnerPriority + ": " + providerNames);
    }

    return Objects.requireNonNull(
        instanceExtractor.apply(winner),
        providerType.getSimpleName() + " returned null runtime component");
  }
}
