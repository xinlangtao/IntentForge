package cn.intentforge.channel.local.plugin;

import cn.intentforge.channel.ChannelDriver;
import cn.intentforge.channel.registry.ChannelManager;
import cn.intentforge.channel.spi.ChannelPlugin;
import cn.intentforge.common.plugin.DirectoryServicePluginLoader;
import cn.intentforge.common.plugin.PluginHandle;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Plugin manager that loads {@link ChannelPlugin} implementations from plugin jars.
 *
 * @since 1.0.0
 */
public final class DirectoryChannelPluginManager {
  /**
   * Channel plugin API version.
   */
  public static final String PLUGIN_API_VERSION = "1";
  /**
   * Runtime version used by compatibility validation.
   */
  public static final String RUNTIME_VERSION = "nightly-SNAPSHOT";

  private final ChannelManager channelManager;
  private final DirectoryServicePluginLoader<ChannelPlugin> pluginLoader;
  private final Map<String, Set<String>> driverIdsByPluginId = new LinkedHashMap<>();

  /**
   * Creates one manager with default API/runtime versions.
   *
   * @param pluginsDirectory plugin directory
   * @param channelManager channel manager
   */
  public DirectoryChannelPluginManager(Path pluginsDirectory, ChannelManager channelManager) {
    this(pluginsDirectory, channelManager, PLUGIN_API_VERSION, RUNTIME_VERSION);
  }

  /**
   * Creates one manager with explicit API/runtime versions.
   *
   * @param pluginsDirectory plugin directory
   * @param channelManager channel manager
   * @param pluginApiVersion plugin API version
   * @param runtimeVersion runtime version
   */
  public DirectoryChannelPluginManager(
      Path pluginsDirectory,
      ChannelManager channelManager,
      String pluginApiVersion,
      String runtimeVersion
  ) {
    this.channelManager = channelManager;
    this.pluginLoader = new DirectoryServicePluginLoader<>(
        pluginsDirectory,
        ChannelPlugin.class,
        pluginApiVersion,
        runtimeVersion);
  }

  /**
   * Loads all plugins and syncs their drivers.
   *
   * @return plugin handles
   */
  public synchronized List<PluginHandle<ChannelPlugin>> loadAll() {
    List<String> existingPluginIds = new ArrayList<>(driverIdsByPluginId.keySet());
    List<PluginHandle<ChannelPlugin>> handles = pluginLoader.loadAll();
    List<String> currentPluginIds = new ArrayList<>();
    for (PluginHandle<ChannelPlugin> handle : handles) {
      String pluginId = handle.metadata().id();
      currentPluginIds.add(pluginId);
      sync(handle);
    }
    for (String pluginId : existingPluginIds) {
      if (!currentPluginIds.contains(pluginId)) {
        unregister(pluginId);
      }
    }
    return handles;
  }

  /**
   * Starts one plugin and syncs its drivers.
   *
   * @param pluginId plugin identifier
   * @return plugin handle when present
   */
  public synchronized Optional<PluginHandle<ChannelPlugin>> start(String pluginId) {
    Optional<PluginHandle<ChannelPlugin>> handle = pluginLoader.start(pluginId);
    handle.ifPresent(this::sync);
    return handle;
  }

  /**
   * Stops one plugin and unregisters its drivers.
   *
   * @param pluginId plugin identifier
   * @return plugin handle when present
   */
  public synchronized Optional<PluginHandle<ChannelPlugin>> stop(String pluginId) {
    unregister(pluginId);
    return pluginLoader.stop(pluginId);
  }

  /**
   * Lists plugin handles.
   *
   * @return plugin handles
   */
  public synchronized List<PluginHandle<ChannelPlugin>> plugins() {
    return pluginLoader.list();
  }

  private void sync(PluginHandle<ChannelPlugin> handle) {
    String pluginId = handle.metadata().id();
    unregister(pluginId);
    if (!handle.active()) {
      return;
    }
    Set<String> driverIds = new LinkedHashSet<>();
    List<ChannelDriver> drivers = handle.services()
        .stream()
        .map(ChannelPlugin::drivers)
        .filter(collection -> collection != null && !collection.isEmpty())
        .flatMap(collection -> collection.stream())
        .toList();
    for (ChannelDriver driver : drivers) {
      channelManager.register(driver);
      driverIds.add(driver.descriptor().id());
    }
    driverIdsByPluginId.put(pluginId, driverIds);
  }

  private void unregister(String pluginId) {
    Set<String> driverIds = driverIdsByPluginId.remove(pluginId);
    if (driverIds == null || driverIds.isEmpty()) {
      return;
    }
    for (String driverId : driverIds) {
      channelManager.unregister(driverId);
    }
  }
}
