package cn.intentforge.channel.local.registry;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelDeliveryResult;
import cn.intentforge.channel.ChannelDescriptor;
import cn.intentforge.channel.ChannelDriver;
import cn.intentforge.channel.ChannelOutboundRequest;
import cn.intentforge.channel.ChannelSession;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.spi.ChannelPlugin;
import cn.intentforge.channel.spi.ChannelPluginDiscoveryStrategy;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class StaticTestChannelPluginDiscoveryStrategy implements ChannelPluginDiscoveryStrategy {
  @Override
  public Collection<ChannelPlugin> load(ClassLoader classLoader) {
    return List.of(new StrategyTestChannelPlugin());
  }

  private static final class StrategyTestChannelPlugin implements ChannelPlugin {
    @Override
    public Collection<ChannelDriver> drivers() {
      return List.of(new StrategyTestChannelDriver());
    }
  }

  private static final class StrategyTestChannelDriver implements ChannelDriver {
    private static final ChannelDescriptor DESCRIPTOR =
        new ChannelDescriptor("strategy-test", ChannelType.CUSTOM, "Strategy Test", List.of(), Map.of());

    @Override
    public ChannelDescriptor descriptor() {
      return DESCRIPTOR;
    }

    @Override
    public ChannelSession openSession(ChannelAccountProfile accountProfile) {
      return new ChannelSession() {
        @Override
        public ChannelAccountProfile accountProfile() {
          return accountProfile;
        }

        @Override
        public ChannelDeliveryResult send(ChannelOutboundRequest request) {
          return new ChannelDeliveryResult("strategy", "strategy", Instant.EPOCH, Map.of());
        }
      };
    }
  }
}
