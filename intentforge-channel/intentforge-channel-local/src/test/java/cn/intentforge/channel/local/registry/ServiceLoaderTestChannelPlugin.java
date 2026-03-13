package cn.intentforge.channel.local.registry;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelDeliveryResult;
import cn.intentforge.channel.ChannelDescriptor;
import cn.intentforge.channel.ChannelDriver;
import cn.intentforge.channel.ChannelOutboundRequest;
import cn.intentforge.channel.ChannelSession;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.spi.ChannelPlugin;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ServiceLoaderTestChannelPlugin implements ChannelPlugin {
  @Override
  public Collection<ChannelDriver> drivers() {
    return List.of(new ServiceLoaderTestChannelDriver());
  }

  private static final class ServiceLoaderTestChannelDriver implements ChannelDriver {
    private static final ChannelDescriptor DESCRIPTOR =
        new ChannelDescriptor("service-loader-test", ChannelType.CUSTOM, "Service Loader Test", List.of(), Map.of());

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
          return new ChannelDeliveryResult("service-loader", "service-loader", Instant.EPOCH, Map.of());
        }
      };
    }
  }
}
