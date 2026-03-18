package cn.intentforge.channel.wecom.driver;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelCapability;
import cn.intentforge.channel.ChannelDeliveryResult;
import cn.intentforge.channel.ChannelOutboundRequest;
import cn.intentforge.channel.ChannelSession;
import cn.intentforge.channel.ChannelTarget;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.wecom.outbound.WeComRobotApiClient;
import cn.intentforge.channel.wecom.outbound.WeComRobotSendMessageCommand;
import cn.intentforge.channel.wecom.outbound.WeComRobotSendMessageResult;
import cn.intentforge.channel.wecom.plugin.WeComChannelPlugin;
import cn.intentforge.channel.wecom.shared.WeComPropertyNames;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WeComChannelPluginTest {
  @Test
  void shouldExposeWeComDriverDescriptor() {
    WeComChannelPlugin plugin = new WeComChannelPlugin();

    Assertions.assertEquals(1, plugin.drivers().size());
    Assertions.assertEquals("intentforge.channel.wecom", plugin.drivers().iterator().next().descriptor().id());
    Assertions.assertEquals(ChannelType.WECOM, plugin.drivers().iterator().next().descriptor().type());
    Assertions.assertTrue(plugin.drivers().iterator().next().descriptor().supports(ChannelCapability.SEND_MESSAGES));
    Assertions.assertTrue(plugin.drivers().iterator().next().descriptor().supports(ChannelCapability.RECEIVE_MESSAGES));
  }

  @Test
  void shouldMapOutboundRequestToRobotSendCommand() {
    RecordingWeComRobotApiClient apiClient = new RecordingWeComRobotApiClient();
    WeComChannelDriver driver = new WeComChannelDriver(apiClient);
    ChannelAccountProfile accountProfile = new ChannelAccountProfile(
        "wecom-robot",
        ChannelType.WECOM,
        "WeCom Robot",
        Map.of(
            WeComPropertyNames.CALLBACK_TOKEN, "robot-token",
            WeComPropertyNames.CALLBACK_ENCODING_AES_KEY, "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG",
            WeComPropertyNames.ROBOT_ID, "robot-123",
            WeComPropertyNames.ROBOT_SECRET, "robot-secret"));

    ChannelSession session = driver.openSession(accountProfile);
    ChannelDeliveryResult result = session.send(new ChannelOutboundRequest(
        new ChannelTarget("wecom-robot", "chat-1", null, "zhangsan", Map.of()),
        "hello wecom robot",
        Map.of("sessionId", "session-1")));

    Assertions.assertEquals("robot-123", apiClient.command.robotId());
    Assertions.assertEquals("robot-secret", apiClient.command.robotSecret());
    Assertions.assertEquals("chat-1", apiClient.command.chatId());
    Assertions.assertEquals("zhangsan", apiClient.command.userId());
    Assertions.assertEquals("session-1", apiClient.command.sessionId());
    Assertions.assertEquals("hello wecom robot", apiClient.command.text());
    Assertions.assertEquals("wecom-robot:message-1", result.deliveryId());
    Assertions.assertEquals("message-1", result.externalMessageId());
  }

  @Test
  void shouldRejectMissingRobotCredentials() {
    WeComChannelDriver driver = new WeComChannelDriver(new RecordingWeComRobotApiClient());

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> driver.openSession(new ChannelAccountProfile(
            "wecom-robot",
            ChannelType.WECOM,
            "WeCom Robot",
            Map.of(WeComPropertyNames.ROBOT_ID, "robot-123"))));
  }

  private static final class RecordingWeComRobotApiClient implements WeComRobotApiClient {
    private WeComRobotSendMessageCommand command;

    @Override
    public WeComRobotSendMessageResult sendText(WeComRobotSendMessageCommand command) {
      this.command = command;
      return new WeComRobotSendMessageResult("message-1", Instant.ofEpochSecond(1_700_000_200L));
    }
  }
}
