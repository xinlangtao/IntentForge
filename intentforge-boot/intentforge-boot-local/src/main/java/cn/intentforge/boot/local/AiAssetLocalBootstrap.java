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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
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
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

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

    return new AiAssetLocalRuntime(
        promptManager,
        promptPluginManager,
        modelManager,
        modelPluginManager,
        providerRegistry,
        providerPluginManager);
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
