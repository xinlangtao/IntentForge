package cn.intentforge.channel.connectors;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelCapability;
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

/**
 * Builtin loopback channel plugin used as a stable classpath connector example.
 *
 * @since 1.0.0
 */
public final class LoopbackChannelPlugin implements ChannelPlugin {
  private static final ChannelDescriptor LOOPBACK_DESCRIPTOR = new ChannelDescriptor(
      "intentforge.channel.loopback",
      ChannelType.LOOPBACK,
      "Loopback Channel",
      List.of(ChannelCapability.SEND_MESSAGES, ChannelCapability.RECEIVE_MESSAGES),
      Map.of("builtin", "true"));

  /**
   * Returns the builtin loopback driver.
   *
   * @return loopback driver contribution
   */
  @Override
  public Collection<ChannelDriver> drivers() {
    return List.of(new LoopbackChannelDriver());
  }

  private static final class LoopbackChannelDriver implements ChannelDriver {
    @Override
    public ChannelDescriptor descriptor() {
      return LOOPBACK_DESCRIPTOR;
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
          return new ChannelDeliveryResult(
              "loopback:" + request.text(),
              "loopback:" + request.target().conversationId(),
              Instant.EPOCH,
              Map.of("driver", LOOPBACK_DESCRIPTOR.id()));
        }
      };
    }
  }
}
