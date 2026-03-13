package cn.intentforge.channel.spring;

import cn.intentforge.channel.spi.ChannelPlugin;
import cn.intentforge.channel.spi.ChannelPluginDiscoveryStrategy;
import java.util.List;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Loads {@link ChannelPlugin} implementations via Spring {@code spring.factories}.
 *
 * @since 1.0.0
 */
public final class SpringFactoriesChannelPluginDiscoveryStrategy implements ChannelPluginDiscoveryStrategy {
  /**
   * Discovers channel plugins via Spring's factory loading mechanism.
   *
   * @param classLoader class loader used for discovery
   * @return discovered channel plugins
   */
  @Override
  public List<ChannelPlugin> load(ClassLoader classLoader) {
    ClassLoader effectiveClassLoader = classLoader == null
        ? SpringFactoriesChannelPluginDiscoveryStrategy.class.getClassLoader()
        : classLoader;
    return List.copyOf(SpringFactoriesLoader.loadFactories(ChannelPlugin.class, effectiveClassLoader));
  }
}
