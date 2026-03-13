package cn.intentforge.channel.registry;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelDriver;
import cn.intentforge.channel.ChannelSession;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Registry and runtime facade for pluggable channel drivers.
 *
 * @since 1.0.0
 */
public interface ChannelManager {
  /**
   * Registers one driver.
   *
   * @param driver channel driver
   */
  void register(ChannelDriver driver);

  /**
   * Registers all provided drivers.
   *
   * @param drivers channel drivers
   */
  void registerAll(Collection<ChannelDriver> drivers);

  /**
   * Unregisters one driver by identifier.
   *
   * @param driverId driver identifier
   */
  void unregister(String driverId);

  /**
   * Finds one driver by identifier.
   *
   * @param driverId driver identifier
   * @return matching driver when present
   */
  Optional<ChannelDriver> find(String driverId);

  /**
   * Finds one driver that supports the provided account profile.
   *
   * @param accountProfile account profile
   * @return matching driver when present
   */
  Optional<ChannelDriver> findDriver(ChannelAccountProfile accountProfile);

  /**
   * Opens one session for the provided account profile.
   *
   * @param accountProfile account profile
   * @return opened session when a supporting driver exists
   */
  Optional<ChannelSession> openSession(ChannelAccountProfile accountProfile);

  /**
   * Lists registered drivers.
   *
   * @return immutable driver list
   */
  List<ChannelDriver> list();

  /**
   * Loads classpath channel plugins.
   */
  void loadPlugins();
}
