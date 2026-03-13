package cn.intentforge.channel;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ChannelDescriptorTest {
  @Test
  void shouldNormalizeDescriptorValues() {
    ChannelDescriptor descriptor = new ChannelDescriptor(
        " telegram-driver ",
        ChannelType.TELEGRAM,
        " Telegram Driver ",
        List.of(ChannelCapability.SEND_MESSAGES, ChannelCapability.RECEIVE_MESSAGES, ChannelCapability.SEND_MESSAGES),
        Map.of(" priority ", " high "));

    Assertions.assertEquals("telegram-driver", descriptor.id());
    Assertions.assertEquals(ChannelType.TELEGRAM, descriptor.type());
    Assertions.assertEquals("Telegram Driver", descriptor.displayName());
    Assertions.assertEquals(
        List.of(ChannelCapability.SEND_MESSAGES, ChannelCapability.RECEIVE_MESSAGES),
        descriptor.capabilities());
    Assertions.assertEquals(Map.of("priority", "high"), descriptor.metadata());
  }

  @Test
  void shouldRejectBlankIdentifier() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new ChannelDescriptor(" ", ChannelType.TELEGRAM, "Telegram", List.of(), Map.of()));
  }
}
