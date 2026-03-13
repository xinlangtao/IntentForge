package cn.intentforge.tool.core.registry;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import cn.intentforge.tool.core.spi.ToolPlugin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * In-memory tool registry with classpath SPI loading support.
 */
public final class InMemoryToolRegistry implements ToolRegistry {
  private final ClassLoader classLoader;
  private final Map<String, ToolRegistration> registrationsById = new LinkedHashMap<>();

  private boolean pluginsLoaded;

  /**
   * Creates the registry with current thread context class loader.
   */
  public InMemoryToolRegistry() {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Creates the registry with provided class loader.
   *
   * @param classLoader class loader for SPI loading
   */
  public InMemoryToolRegistry(ClassLoader classLoader) {
    this.classLoader = classLoader == null ? InMemoryToolRegistry.class.getClassLoader() : classLoader;
  }

  /**
   * Creates registry and loads plugins immediately.
   *
   * @return loaded registry
   */
  public static InMemoryToolRegistry createAndLoad() {
    InMemoryToolRegistry registry = new InMemoryToolRegistry();
    registry.loadPlugins();
    return registry;
  }

  @Override
  public synchronized void register(ToolRegistration registration) {
    ToolRegistration nonNullRegistration = requireRegistration(registration);
    registrationsById.put(nonNullRegistration.definition().id(), nonNullRegistration);
  }

  @Override
  public synchronized void registerAll(Collection<ToolRegistration> registrations) {
    if (registrations == null || registrations.isEmpty()) {
      return;
    }
    for (ToolRegistration registration : registrations) {
      register(registration);
    }
  }

  @Override
  public synchronized void unregister(String toolId) {
    String normalizedToolId = normalize(toolId);
    if (normalizedToolId == null) {
      return;
    }
    registrationsById.remove(normalizedToolId);
  }

  @Override
  public synchronized Optional<ToolRegistration> find(String toolId) {
    String normalizedToolId = normalize(toolId);
    if (normalizedToolId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(registrationsById.get(normalizedToolId));
  }

  @Override
  public synchronized List<ToolRegistration> list() {
    return List.copyOf(new ArrayList<>(registrationsById.values()));
  }

  @Override
  public synchronized void loadPlugins() {
    if (pluginsLoaded) {
      return;
    }
    List<ToolRegistration> registrations = ServiceLoader.load(ToolPlugin.class, classLoader)
        .stream()
        .map(ServiceLoader.Provider::get)
        .map(ToolPlugin::tools)
        .filter(collection -> collection != null && !collection.isEmpty())
        .flatMap(Collection::stream)
        .toList();
    registerAll(registrations);
    pluginsLoaded = true;
  }

  private static ToolRegistration requireRegistration(ToolRegistration registration) {
    if (registration == null) {
      throw new IllegalArgumentException("registration must not be null");
    }
    return registration;
  }
}
