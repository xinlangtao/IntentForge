package cn.intentforge.model.provider.local.registry;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import cn.intentforge.model.catalog.ModelDescriptor;
import cn.intentforge.model.provider.ModelProvider;
import cn.intentforge.model.provider.ModelProviderDescriptor;
import cn.intentforge.model.provider.registry.ModelProviderRegistry;
import cn.intentforge.model.provider.spi.ModelProviderPlugin;
import cn.intentforge.model.registry.ModelManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

public final class InMemoryModelProviderRegistry implements ModelProviderRegistry {
  private final ClassLoader classLoader;
  private final Map<String, ModelProvider> providersById = new LinkedHashMap<>();

  private boolean pluginsLoaded;

  public InMemoryModelProviderRegistry() {
    this(Thread.currentThread().getContextClassLoader());
  }

  public InMemoryModelProviderRegistry(ClassLoader classLoader) {
    this.classLoader = classLoader == null ? InMemoryModelProviderRegistry.class.getClassLoader() : classLoader;
  }

  public static InMemoryModelProviderRegistry createAndLoad() {
    InMemoryModelProviderRegistry registry = new InMemoryModelProviderRegistry();
    registry.loadPlugins();
    return registry;
  }

  @Override
  public synchronized void register(ModelProvider modelProvider) {
    ModelProvider provider = Objects.requireNonNull(modelProvider, "modelProvider must not be null");
    providersById.put(provider.descriptor().id(), provider);
  }

  @Override
  public synchronized void registerAll(Collection<? extends ModelProvider> modelProviders) {
    if (modelProviders == null || modelProviders.isEmpty()) {
      return;
    }
    for (ModelProvider modelProvider : modelProviders) {
      register(modelProvider);
    }
  }

  @Override
  public synchronized void unregister(String id) {
    String normalizedId = normalize(id);
    if (normalizedId == null) {
      return;
    }
    providersById.remove(normalizedId);
  }

  @Override
  public synchronized void unregisterAll(Collection<? extends ModelProvider> modelProviders) {
    if (modelProviders == null || modelProviders.isEmpty()) {
      return;
    }
    for (ModelProvider modelProvider : modelProviders) {
      if (modelProvider == null) {
        continue;
      }
      unregister(modelProvider.descriptor().id());
    }
  }

  @Override
  public synchronized Optional<ModelProvider> find(String id) {
    String normalizedId = normalize(id);
    if (normalizedId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(providersById.get(normalizedId));
  }

  @Override
  public synchronized List<ModelProviderDescriptor> list() {
    List<ModelProviderDescriptor> descriptors = new ArrayList<>();
    for (ModelProvider provider : providersById.values()) {
      descriptors.add(provider.descriptor());
    }
    return List.copyOf(descriptors);
  }

  @Override
  public synchronized void registerSupportedModels(ModelManager modelManager) {
    ModelManager manager = Objects.requireNonNull(modelManager, "modelManager must not be null");
    for (ModelProvider provider : providersById.values()) {
      Collection<ModelDescriptor> supportedModels = provider.supportedModels();
      if (supportedModels == null || supportedModels.isEmpty()) {
        continue;
      }
      manager.registerAll(List.copyOf(supportedModels));
    }
  }

  @Override
  public synchronized void loadPlugins() {
    if (pluginsLoaded) {
      return;
    }
    List<ModelProvider> providers = ServiceLoader.load(ModelProviderPlugin.class, classLoader)
        .stream()
        .map(ServiceLoader.Provider::get)
        .map(ModelProviderPlugin::provider)
        .filter(ModelProvider::available)
        .toList();
    registerAll(providers);
    pluginsLoaded = true;
  }
}
