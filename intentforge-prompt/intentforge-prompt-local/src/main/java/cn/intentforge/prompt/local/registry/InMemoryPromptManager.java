package cn.intentforge.prompt.local.registry;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import cn.intentforge.prompt.model.PromptDefinition;
import cn.intentforge.prompt.model.PromptQuery;
import cn.intentforge.prompt.registry.PromptManager;
import cn.intentforge.prompt.spi.PromptPlugin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

public final class InMemoryPromptManager implements PromptManager {
  private final ClassLoader classLoader;
  private final Map<String, LinkedHashMap<String, PromptDefinition>> promptsById = new LinkedHashMap<>();

  private boolean pluginsLoaded;

  public InMemoryPromptManager() {
    this(Thread.currentThread().getContextClassLoader());
  }

  public InMemoryPromptManager(ClassLoader classLoader) {
    this.classLoader = classLoader == null ? InMemoryPromptManager.class.getClassLoader() : classLoader;
  }

  public static InMemoryPromptManager createAndLoad() {
    InMemoryPromptManager manager = new InMemoryPromptManager();
    manager.loadPlugins();
    return manager;
  }

  @Override
  public synchronized void register(PromptDefinition promptDefinition) {
    PromptDefinition prompt = Objects.requireNonNull(promptDefinition, "promptDefinition must not be null");
    promptsById
        .computeIfAbsent(prompt.id(), ignored -> new LinkedHashMap<>())
        .put(prompt.version(), prompt);
  }

  @Override
  public synchronized void registerAll(Collection<PromptDefinition> promptDefinitions) {
    if (promptDefinitions == null || promptDefinitions.isEmpty()) {
      return;
    }
    for (PromptDefinition promptDefinition : promptDefinitions) {
      register(promptDefinition);
    }
  }

  @Override
  public synchronized void unregister(String id, String version) {
    String normalizedId = normalize(id);
    String normalizedVersion = normalize(version);
    if (normalizedId == null || normalizedVersion == null) {
      return;
    }
    LinkedHashMap<String, PromptDefinition> versions = promptsById.get(normalizedId);
    if (versions == null) {
      return;
    }
    versions.remove(normalizedVersion);
    if (versions.isEmpty()) {
      promptsById.remove(normalizedId);
    }
  }

  @Override
  public synchronized void unregisterAll(Collection<PromptDefinition> promptDefinitions) {
    if (promptDefinitions == null || promptDefinitions.isEmpty()) {
      return;
    }
    for (PromptDefinition promptDefinition : promptDefinitions) {
      if (promptDefinition == null) {
        continue;
      }
      unregister(promptDefinition.id(), promptDefinition.version());
    }
  }

  @Override
  public synchronized Optional<PromptDefinition> find(String id, String version) {
    String normalizedId = normalize(id);
    if (normalizedId == null) {
      return Optional.empty();
    }
    LinkedHashMap<String, PromptDefinition> versions = promptsById.get(normalizedId);
    if (versions == null || versions.isEmpty()) {
      return Optional.empty();
    }
    String normalizedVersion = normalize(version);
    if (normalizedVersion == null) {
      return findLatest(normalizedId);
    }
    return Optional.ofNullable(versions.get(normalizedVersion));
  }

  @Override
  public synchronized Optional<PromptDefinition> findLatest(String id) {
    String normalizedId = normalize(id);
    if (normalizedId == null) {
      return Optional.empty();
    }
    LinkedHashMap<String, PromptDefinition> versions = promptsById.get(normalizedId);
    if (versions == null || versions.isEmpty()) {
      return Optional.empty();
    }
    PromptDefinition latest = null;
    for (PromptDefinition promptDefinition : versions.values()) {
      latest = promptDefinition;
    }
    return Optional.ofNullable(latest);
  }

  @Override
  public synchronized List<PromptDefinition> list(PromptQuery query) {
    List<PromptDefinition> promptDefinitions = new ArrayList<>();
    for (LinkedHashMap<String, PromptDefinition> versions : promptsById.values()) {
      for (PromptDefinition promptDefinition : versions.values()) {
        if (promptDefinition.matches(query)) {
          promptDefinitions.add(promptDefinition);
        }
      }
    }
    return List.copyOf(promptDefinitions);
  }

  @Override
  public synchronized void loadPlugins() {
    if (pluginsLoaded) {
      return;
    }
    ServiceLoader.load(PromptPlugin.class, classLoader)
        .stream()
        .map(ServiceLoader.Provider::get)
        .map(PromptPlugin::prompts)
        .forEach(this::registerAll);
    pluginsLoaded = true;
  }
}
