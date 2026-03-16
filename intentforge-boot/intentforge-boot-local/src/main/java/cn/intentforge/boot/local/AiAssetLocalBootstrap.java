package cn.intentforge.boot.local;

import cn.intentforge.agent.core.AgentGateway;
import cn.intentforge.agent.core.AgentRunGateway;
import cn.intentforge.agent.nativejava.NativeCodingAgentFactory;
import cn.intentforge.channel.ChannelInboundProcessor;
import cn.intentforge.channel.local.inbound.DefaultChannelInboundProcessor;
import cn.intentforge.channel.local.plugin.DirectoryChannelPluginManager;
import cn.intentforge.channel.registry.ChannelManager;
import cn.intentforge.channel.spi.ChannelManagerProvider;
import cn.intentforge.config.ResolvedRuntimeSelection;
import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.config.RuntimeCatalog;
import cn.intentforge.config.RuntimeImplementationDescriptor;
import cn.intentforge.governance.agent.DefaultAgentGateway;
import cn.intentforge.governance.agent.DefaultAgentRunGateway;
import cn.intentforge.governance.agent.StageRoutingAgentRouter;
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
import cn.intentforge.session.registry.SessionManager;
import cn.intentforge.session.spi.SessionManagerProvider;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    List<ChannelManagerProvider> channelManagerProviders = loadProviders(classLoader, ChannelManagerProvider.class);
    Map<String, ChannelManager> channelManagers = instantiateComponents(
        channelManagerProviders,
        ChannelManagerProvider::descriptor,
        provider -> provider.create(classLoader));
    Map<String, DirectoryChannelPluginManager> channelPluginManagers = new LinkedHashMap<>();
    for (Map.Entry<String, ChannelManager> entry : channelManagers.entrySet()) {
      entry.getValue().loadPlugins();
      DirectoryChannelPluginManager pluginManager = new DirectoryChannelPluginManager(pluginsDirectory, entry.getValue());
      pluginManager.loadAll();
      channelPluginManagers.put(entry.getKey(), pluginManager);
    }

    List<SessionManagerProvider> sessionManagerProviders = loadProviders(classLoader, SessionManagerProvider.class);
    Map<String, SessionManager> sessionManagers = instantiateComponents(
        sessionManagerProviders,
        SessionManagerProvider::descriptor,
        provider -> provider.create(classLoader));

    List<PromptManagerProvider> promptManagerProviders = loadProviders(classLoader, PromptManagerProvider.class);
    Map<String, PromptManager> promptManagers = instantiateComponents(
        promptManagerProviders,
        PromptManagerProvider::descriptor,
        provider -> provider.create(classLoader));
    Map<String, DirectoryPromptPluginManager> promptPluginManagers = new LinkedHashMap<>();
    for (Map.Entry<String, PromptManager> entry : promptManagers.entrySet()) {
      entry.getValue().loadPlugins();
      DirectoryPromptPluginManager pluginManager = new DirectoryPromptPluginManager(pluginsDirectory, entry.getValue());
      pluginManager.loadAll();
      promptPluginManagers.put(entry.getKey(), pluginManager);
    }

    List<ModelManagerProvider> modelManagerProviders = loadProviders(classLoader, ModelManagerProvider.class);
    Map<String, ModelManager> modelManagers = instantiateComponents(
        modelManagerProviders,
        ModelManagerProvider::descriptor,
        provider -> provider.create(classLoader));
    Map<String, DirectoryModelPluginManager> modelPluginManagers = new LinkedHashMap<>();
    for (Map.Entry<String, ModelManager> entry : modelManagers.entrySet()) {
      entry.getValue().loadPlugins();
      DirectoryModelPluginManager pluginManager = new DirectoryModelPluginManager(pluginsDirectory, entry.getValue());
      pluginManager.loadAll();
      modelPluginManagers.put(entry.getKey(), pluginManager);
    }

    List<ModelProviderRegistryProvider> providerRegistryProviders = loadProviders(classLoader, ModelProviderRegistryProvider.class);
    Map<String, ModelProviderRegistry> providerRegistries = instantiateComponents(
        providerRegistryProviders,
        ModelProviderRegistryProvider::descriptor,
        provider -> provider.create(classLoader));

    List<ToolRegistryProvider> toolRegistryProviders = loadProviders(classLoader, ToolRegistryProvider.class);
    Map<String, ToolRegistry> toolRegistries = instantiateComponents(
        toolRegistryProviders,
        ToolRegistryProvider::descriptor,
        provider -> provider.create(classLoader));
    Map<String, DirectoryToolPluginManager> toolPluginManagers = new LinkedHashMap<>();
    for (Map.Entry<String, ToolRegistry> entry : toolRegistries.entrySet()) {
      entry.getValue().loadPlugins();
      DirectoryToolPluginManager pluginManager = new DirectoryToolPluginManager(pluginsDirectory, entry.getValue());
      pluginManager.loadAll();
      toolPluginManagers.put(entry.getKey(), pluginManager);
    }

    RuntimeCatalog runtimeCatalog = RuntimeCatalog.of(concatDescriptors(
        channelManagerProviders.stream().map(ChannelManagerProvider::descriptor).toList(),
        promptManagerProviders.stream().map(PromptManagerProvider::descriptor).toList(),
        modelManagerProviders.stream().map(ModelManagerProvider::descriptor).toList(),
        providerRegistryProviders.stream().map(ModelProviderRegistryProvider::descriptor).toList(),
        toolRegistryProviders.stream().map(ToolRegistryProvider::descriptor).toList(),
        sessionManagerProviders.stream().map(SessionManagerProvider::descriptor).toList()));

    DefaultToolPermissionPolicy toolPermissionPolicy = new DefaultToolPermissionPolicy(DEFAULT_SENSITIVE_TOOL_IDS);
    Map<String, ToolGateway> toolGateways = new LinkedHashMap<>();
    for (Map.Entry<String, ToolRegistry> entry : toolRegistries.entrySet()) {
      toolGateways.put(entry.getKey(), new DefaultToolGateway(entry.getValue(), toolPermissionPolicy));
    }

    LocalRuntimeComponentRegistry runtimeComponents = new LocalRuntimeComponentRegistry(
        channelManagers,
        promptManagers,
        modelManagers,
        providerRegistries,
        toolRegistries,
        toolGateways,
        sessionManagers);
    RuntimeImplementationDescriptor defaultPromptDescriptor = requireDefaultDescriptor(runtimeCatalog, RuntimeCapability.PROMPT_MANAGER);
    RuntimeImplementationDescriptor defaultModelDescriptor = requireDefaultDescriptor(runtimeCatalog, RuntimeCapability.MODEL_MANAGER);
    RuntimeImplementationDescriptor defaultProviderDescriptor =
        requireDefaultDescriptor(runtimeCatalog, RuntimeCapability.MODEL_PROVIDER_REGISTRY);
    RuntimeImplementationDescriptor defaultToolDescriptor = requireDefaultDescriptor(runtimeCatalog, RuntimeCapability.TOOL_REGISTRY);
    RuntimeImplementationDescriptor defaultSessionDescriptor = requireDefaultDescriptor(runtimeCatalog, RuntimeCapability.SESSION_MANAGER);
    RuntimeImplementationDescriptor defaultChannelDescriptor = requireDefaultDescriptor(runtimeCatalog, RuntimeCapability.CHANNEL_MANAGER);

    ChannelManager channelManager = runtimeComponents.channelManager(defaultChannelDescriptor.id());
    DirectoryChannelPluginManager channelPluginManager = channelPluginManagers.get(defaultChannelDescriptor.id());
    PromptManager promptManager = runtimeComponents.promptManager(defaultPromptDescriptor.id());
    DirectoryPromptPluginManager promptPluginManager = promptPluginManagers.get(defaultPromptDescriptor.id());
    ModelManager modelManager = runtimeComponents.modelManager(defaultModelDescriptor.id());
    DirectoryModelPluginManager modelPluginManager = modelPluginManagers.get(defaultModelDescriptor.id());
    Map<String, DirectoryModelProviderPluginManager> providerPluginManagers = new LinkedHashMap<>();
    for (Map.Entry<String, ModelProviderRegistry> entry : providerRegistries.entrySet()) {
      entry.getValue().loadPlugins();
      for (Map.Entry<String, ModelManager> modelEntry : modelManagers.entrySet()) {
        DirectoryModelProviderPluginManager pluginManager = new DirectoryModelProviderPluginManager(
            pluginsDirectory,
            entry.getValue(),
            modelEntry.getValue());
        pluginManager.loadAll();
        if (entry.getKey().equals(defaultProviderDescriptor.id()) && modelEntry.getKey().equals(defaultModelDescriptor.id())) {
          providerPluginManagers.put(entry.getKey(), pluginManager);
        }
      }
    }
    ModelProviderRegistry providerRegistry = runtimeComponents.modelProviderRegistry(defaultProviderDescriptor.id());
    DirectoryModelProviderPluginManager providerPluginManager = providerPluginManagers.get(defaultProviderDescriptor.id());
    ToolRegistry toolRegistry = runtimeComponents.toolRegistry(defaultToolDescriptor.id());
    DirectoryToolPluginManager toolPluginManager = toolPluginManagers.get(defaultToolDescriptor.id());
    ToolGateway toolGateway = runtimeComponents.toolGateway(defaultToolDescriptor.id());
    SessionManager sessionManager = runtimeComponents.sessionManager(defaultSessionDescriptor.id());
    ChannelInboundProcessor channelInboundProcessor = new PersistingChannelInboundProcessor(
        DefaultChannelInboundProcessor.createAndLoad(channelManager, classLoader),
        sessionManager);
    providerPluginManager = providerPluginManagers.get(defaultProviderDescriptor.id());

    LocalRuntimeComponentResolver runtimeResolver = new LocalRuntimeComponentResolver(
        runtimeCatalog,
        runtimeComponents,
        new ResolvedRuntimeSelection(Map.of(
            RuntimeCapability.SESSION_MANAGER, defaultSessionDescriptor,
            RuntimeCapability.CHANNEL_MANAGER, defaultChannelDescriptor)));

    List<cn.intentforge.agent.core.AgentExecutor> executors = NativeCodingAgentFactory.createDefaultExecutors();
    AgentRunGateway agentRunGateway = new DefaultAgentRunGateway(
        sessionManager,
        spaceResolver,
        runtimeResolver,
        new StageRoutingAgentRouter(),
        executors);
    AgentGateway agentGateway = new DefaultAgentGateway(agentRunGateway, executors);

    return new AiAssetLocalRuntime(
        runtimeCatalog,
        runtimeComponents,
        channelManager,
        channelInboundProcessor,
        channelPluginManager,
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
        agentGateway,
        agentRunGateway,
        sessionManager,
        spaceRegistry,
        spaceResolver);
  }

  private static RuntimeImplementationDescriptor requireDefaultDescriptor(
      RuntimeCatalog runtimeCatalog,
      RuntimeCapability capability
  ) {
    return runtimeCatalog.defaultImplementation(capability)
        .orElseThrow(() -> new IllegalStateException("no default runtime implementation available for capability " + capability));
  }

  private static <P, R> Map<String, R> instantiateComponents(
      List<P> providers,
      Function<P, RuntimeImplementationDescriptor> descriptorExtractor,
      Function<P, R> instanceExtractor
  ) {
    Map<String, R> components = new LinkedHashMap<>();
    for (P provider : providers) {
      RuntimeImplementationDescriptor descriptor = Objects.requireNonNull(
          descriptorExtractor.apply(provider),
          "runtime implementation descriptor must not be null");
      R component = Objects.requireNonNull(
          instanceExtractor.apply(provider),
          "runtime component must not be null");
      R previous = components.putIfAbsent(descriptor.id(), component);
      if (previous != null) {
        throw new IllegalStateException("duplicate runtime implementation id: " + descriptor.id());
      }
    }
    return Map.copyOf(components);
  }

  private static PromptManager createPromptManager(ClassLoader classLoader, List<PromptManagerProvider> providers) {
    return selectSingleProvider(
        providers,
        PromptManagerProvider.class,
        PromptManagerProvider::priority,
        provider -> provider.create(classLoader),
        () -> new InMemoryPromptManager(classLoader));
  }

  private static ModelManager createModelManager(ClassLoader classLoader, List<ModelManagerProvider> providers) {
    return selectSingleProvider(
        providers,
        ModelManagerProvider.class,
        ModelManagerProvider::priority,
        provider -> provider.create(classLoader),
        () -> new InMemoryModelManager(classLoader));
  }

  private static ModelProviderRegistry createProviderRegistry(
      ClassLoader classLoader,
      List<ModelProviderRegistryProvider> providers
  ) {
    return selectSingleProvider(
        providers,
        ModelProviderRegistryProvider.class,
        ModelProviderRegistryProvider::priority,
        provider -> provider.create(classLoader),
        () -> new InMemoryModelProviderRegistry(classLoader));
  }

  private static ToolRegistry createToolRegistry(ClassLoader classLoader, List<ToolRegistryProvider> providers) {
    return selectSingleProvider(
        providers,
        ToolRegistryProvider.class,
        ToolRegistryProvider::priority,
        provider -> provider.create(classLoader),
        () -> new InMemoryToolRegistry(classLoader));
  }

  private static <P> List<P> loadProviders(ClassLoader classLoader, Class<P> providerType) {
    return ServiceLoader.load(providerType, classLoader)
        .stream()
        .map(ServiceLoader.Provider::get)
        .toList();
  }

  @SafeVarargs
  private static List<RuntimeImplementationDescriptor> concatDescriptors(List<RuntimeImplementationDescriptor>... groups) {
    List<RuntimeImplementationDescriptor> descriptors = new ArrayList<>();
    for (List<RuntimeImplementationDescriptor> group : groups) {
      descriptors.addAll(group);
    }
    return List.copyOf(descriptors);
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
