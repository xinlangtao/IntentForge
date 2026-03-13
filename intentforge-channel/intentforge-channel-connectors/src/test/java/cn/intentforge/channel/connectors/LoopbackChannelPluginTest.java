package cn.intentforge.channel.connectors;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelOutboundRequest;
import cn.intentforge.channel.ChannelTarget;
import cn.intentforge.channel.ChannelType;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LoopbackChannelPluginTest {
  @Test
  void shouldCreateLoopbackDriverAndSendMessage() {
    LoopbackChannelPlugin plugin = new LoopbackChannelPlugin();
    ChannelAccountProfile accountProfile =
        new ChannelAccountProfile("loopback-account", ChannelType.LOOPBACK, "Loopback", Map.of());

    Assertions.assertEquals(1, plugin.drivers().size());
    Assertions.assertEquals(
        "loopback:hello",
        plugin.drivers()
            .iterator()
            .next()
            .openSession(accountProfile)
            .send(new ChannelOutboundRequest(new ChannelTarget("loopback-account", "chat-1", null, null, Map.of()), "hello", Map.of()))
            .deliveryId());
  }
}
