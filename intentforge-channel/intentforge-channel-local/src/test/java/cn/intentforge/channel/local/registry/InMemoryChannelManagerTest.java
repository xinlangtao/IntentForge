package cn.intentforge.channel.local.registry;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelDeliveryResult;
import cn.intentforge.channel.ChannelDescriptor;
import cn.intentforge.channel.ChannelDriver;
import cn.intentforge.channel.ChannelOutboundRequest;
import cn.intentforge.channel.ChannelSession;
import cn.intentforge.channel.ChannelTarget;
import cn.intentforge.channel.ChannelType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InMemoryChannelManagerTest {
  @Test
  void shouldRegisterAndOpenSessionForSupportedAccount() {
    InMemoryChannelManager manager = new InMemoryChannelManager();
    manager.register(new StaticTestChannelDriver("manual-driver", ChannelType.TELEGRAM));

    ChannelAccountProfile accountProfile =
        new ChannelAccountProfile("telegram-account", ChannelType.TELEGRAM, "Telegram Bot", Map.of("token", "demo"));
    ChannelOutboundRequest request =
        new ChannelOutboundRequest(new ChannelTarget("telegram-account", "chat-1", null, null, Map.of()), "hello", Map.of());

    ChannelSession session = manager.openSession(accountProfile).orElseThrow();
    ChannelDeliveryResult result = session.send(request);

    Assertions.assertEquals("manual-driver", session.accountProfile().id());
    Assertions.assertEquals("manual-driver:hello", result.deliveryId());
    Assertions.assertTrue(manager.find("manual-driver").isPresent());
  }

  @Test
  void shouldLoadPluginsFromServiceLoaderAndDiscoveryStrategies() {
    InMemoryChannelManager manager = new InMemoryChannelManager();

    manager.loadPlugins();

    Assertions.assertTrue(manager.find("service-loader-test").isPresent());
    Assertions.assertTrue(manager.find("strategy-test").isPresent());
  }

  private static final class StaticTestChannelDriver implements ChannelDriver {
    private final ChannelDescriptor descriptor;

    private StaticTestChannelDriver(String id, ChannelType type) {
      this.descriptor = new ChannelDescriptor(id, type, id, List.of(), Map.of());
    }

    @Override
    public ChannelDescriptor descriptor() {
      return descriptor;
    }

    @Override
    public ChannelSession openSession(ChannelAccountProfile accountProfile) {
      return new ChannelSession() {
        @Override
        public ChannelAccountProfile accountProfile() {
          return new ChannelAccountProfile(descriptor.id(), accountProfile.type(), descriptor.displayName(), Map.of());
        }

        @Override
        public ChannelDeliveryResult send(ChannelOutboundRequest request) {
          return new ChannelDeliveryResult(descriptor.id() + ":" + request.text(), "test", java.time.Instant.EPOCH, Map.of());
        }
      };
    }
  }
}
