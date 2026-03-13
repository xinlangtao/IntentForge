package cn.intentforge.channel.spring;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SpringFactoriesChannelPluginDiscoveryStrategyTest {
  @Test
  void shouldLoadPluginsFromSpringFactories() {
    SpringFactoriesChannelPluginDiscoveryStrategy strategy = new SpringFactoriesChannelPluginDiscoveryStrategy();

    Assertions.assertEquals(1, strategy.load(getClass().getClassLoader()).size());
    Assertions.assertEquals(
        "spring-factories-test",
        strategy.load(getClass().getClassLoader()).iterator().next().drivers().iterator().next().descriptor().id());
  }
}
