package cn.intentforge.channel.local.registry;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelDriver;
import cn.intentforge.channel.ChannelSession;
import cn.intentforge.channel.ChannelWebhookAdministration;
import cn.intentforge.channel.ChannelWebhookHandler;
import cn.intentforge.channel.registry.ChannelManager;
import cn.intentforge.channel.spi.ChannelPlugin;
import cn.intentforge.channel.spi.ChannelPluginDiscoveryStrategy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * In-memory {@link ChannelManager} with classpath SPI loading support.
 *
 * @since 1.0.0
 */
public final class InMemoryChannelManager implements ChannelManager {
  private final ClassLoader classLoader;
  private final Map<String, ChannelDriver> driversById = new LinkedHashMap<>();

  private boolean pluginsLoaded;

  /**
   * Creates one manager with the current thread context class loader.
   */
  public InMemoryChannelManager() {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Creates one manager with the provided class loader.
   *
   * @param classLoader class loader for plugin discovery
   */
  public InMemoryChannelManager(ClassLoader classLoader) {
    this.classLoader = classLoader == null ? InMemoryChannelManager.class.getClassLoader() : classLoader;
  }

  /**
   * Creates one manager and loads classpath plugins immediately.
   *
   * @return loaded manager
   */
  public static InMemoryChannelManager createAndLoad() {
    InMemoryChannelManager manager = new InMemoryChannelManager();
    manager.loadPlugins();
    return manager;
  }

  @Override
  public synchronized void register(ChannelDriver driver) {
    ChannelDriver nonNullDriver = requireDriver(driver);
    driversById.put(nonNullDriver.descriptor().id(), nonNullDriver);
  }

  @Override
  public synchronized void registerAll(Collection<ChannelDriver> drivers) {
    if (drivers == null || drivers.isEmpty()) {
      return;
    }
    for (ChannelDriver driver : drivers) {
      register(driver);
    }
  }

  @Override
  public synchronized void unregister(String driverId) {
    String normalizedDriverId = normalize(driverId);
    if (normalizedDriverId == null) {
      return;
    }
    driversById.remove(normalizedDriverId);
  }

  @Override
  public synchronized Optional<ChannelDriver> find(String driverId) {
    String normalizedDriverId = normalize(driverId);
    if (normalizedDriverId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(driversById.get(normalizedDriverId));
  }

  @Override
  public synchronized Optional<ChannelDriver> findDriver(ChannelAccountProfile accountProfile) {
    if (accountProfile == null) {
      return Optional.empty();
    }
    return driversById.values()
        .stream()
        .filter(driver -> driver.supports(accountProfile))
        .findFirst();
  }

  @Override
  public synchronized Optional<ChannelSession> openSession(ChannelAccountProfile accountProfile) {
    return findDriver(accountProfile)
        .map(driver -> driver.openSession(accountProfile));
  }

  @Override
  public synchronized Optional<ChannelWebhookHandler> openWebhookHandler(ChannelAccountProfile accountProfile) {
    return findDriver(accountProfile)
        .flatMap(driver -> driver.openWebhookHandler(accountProfile));
  }

  @Override
  public synchronized Optional<ChannelWebhookAdministration> openWebhookAdministration(ChannelAccountProfile accountProfile) {
    return findDriver(accountProfile)
        .flatMap(driver -> driver.openWebhookAdministration(accountProfile));
  }

  @Override
  public synchronized List<ChannelDriver> list() {
    return List.copyOf(new ArrayList<>(driversById.values()));
  }

  @Override
  public synchronized void loadPlugins() {
    if (pluginsLoaded) {
      return;
    }
    registerPlugins(loadDirectPlugins());
    for (ChannelPluginDiscoveryStrategy strategy : ServiceLoader.load(ChannelPluginDiscoveryStrategy.class, classLoader)) {
      registerPlugins(strategy.load(classLoader));
    }
    pluginsLoaded = true;
  }

  private List<ChannelPlugin> loadDirectPlugins() {
    return ServiceLoader.load(ChannelPlugin.class, classLoader)
        .stream()
        .map(ServiceLoader.Provider::get)
        .toList();
  }

  private void registerPlugins(Collection<ChannelPlugin> plugins) {
    if (plugins == null || plugins.isEmpty()) {
      return;
    }
    Set<String> loadedPluginClasses = new LinkedHashSet<>();
    for (ChannelPlugin plugin : plugins) {
      if (plugin == null || !loadedPluginClasses.add(plugin.getClass().getName())) {
        continue;
      }
      registerAll(plugin.drivers());
    }
  }

  private static ChannelDriver requireDriver(ChannelDriver driver) {
    if (driver == null) {
      throw new IllegalArgumentException("driver must not be null");
    }
    return driver;
  }
}
