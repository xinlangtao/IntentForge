package cn.intentforge.model.local.registry;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import cn.intentforge.model.catalog.ModelDescriptor;
import cn.intentforge.model.catalog.ModelQuery;
import cn.intentforge.model.registry.ModelManager;
import cn.intentforge.model.spi.ModelPlugin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

public final class InMemoryModelManager implements ModelManager {
  private final ClassLoader classLoader;
  private final Map<String, ModelDescriptor> modelsById = new LinkedHashMap<>();

  private boolean pluginsLoaded;

  public InMemoryModelManager() {
    this(Thread.currentThread().getContextClassLoader());
  }

  public InMemoryModelManager(ClassLoader classLoader) {
    this.classLoader = classLoader == null ? InMemoryModelManager.class.getClassLoader() : classLoader;
  }

  public static InMemoryModelManager createAndLoad() {
    InMemoryModelManager manager = new InMemoryModelManager();
    manager.loadPlugins();
    return manager;
  }

  @Override
  public synchronized void register(ModelDescriptor modelDescriptor) {
    ModelDescriptor model = Objects.requireNonNull(modelDescriptor, "modelDescriptor must not be null");
    modelsById.put(model.id(), model);
  }

  @Override
  public synchronized void registerAll(Collection<ModelDescriptor> modelDescriptors) {
    if (modelDescriptors == null || modelDescriptors.isEmpty()) {
      return;
    }
    for (ModelDescriptor modelDescriptor : modelDescriptors) {
      register(modelDescriptor);
    }
  }

  @Override
  public synchronized void unregister(String id) {
    String normalizedId = normalize(id);
    if (normalizedId == null) {
      return;
    }
    modelsById.remove(normalizedId);
  }

  @Override
  public synchronized void unregisterAll(Collection<ModelDescriptor> modelDescriptors) {
    if (modelDescriptors == null || modelDescriptors.isEmpty()) {
      return;
    }
    for (ModelDescriptor modelDescriptor : modelDescriptors) {
      if (modelDescriptor == null) {
        continue;
      }
      unregister(modelDescriptor.id());
    }
  }

  @Override
  public synchronized Optional<ModelDescriptor> find(String id) {
    String normalizedId = normalize(id);
    if (normalizedId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(modelsById.get(normalizedId));
  }

  @Override
  public synchronized List<ModelDescriptor> list(ModelQuery query) {
    List<ModelDescriptor> modelDescriptors = new ArrayList<>();
    for (ModelDescriptor modelDescriptor : modelsById.values()) {
      if (modelDescriptor.matches(query)) {
        modelDescriptors.add(modelDescriptor);
      }
    }
    return List.copyOf(modelDescriptors);
  }

  @Override
  public synchronized void loadPlugins() {
    if (pluginsLoaded) {
      return;
    }
    ServiceLoader.load(ModelPlugin.class, classLoader)
        .stream()
        .map(ServiceLoader.Provider::get)
        .map(ModelPlugin::models)
        .forEach(this::registerAll);
    pluginsLoaded = true;
  }
}
