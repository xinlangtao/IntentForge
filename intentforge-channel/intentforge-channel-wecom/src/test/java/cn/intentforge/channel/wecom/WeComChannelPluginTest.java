package cn.intentforge.channel.wecom;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelCapability;
import cn.intentforge.channel.ChannelDeliveryResult;
import cn.intentforge.channel.ChannelOutboundRequest;
import cn.intentforge.channel.ChannelSession;
import cn.intentforge.channel.ChannelTarget;
import cn.intentforge.channel.ChannelType;
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
  }

  @Test
  void shouldCacheTokenAndMapOutboundRequestToWeComCommand() {
    RecordingWeComApplicationApiClient apiClient = new RecordingWeComApplicationApiClient();
    WeComChannelDriver driver = new WeComChannelDriver(apiClient);
    ChannelAccountProfile accountProfile = new ChannelAccountProfile(
        "wecom-app",
        ChannelType.WECOM,
        "WeCom App",
        Map.of(
            "corpId", "corp-id",
            "agentId", "1000002",
            "corpSecret", "corp-secret"));

    ChannelSession session = driver.openSession(accountProfile);
    ChannelDeliveryResult firstResult = session.send(new ChannelOutboundRequest(
        new ChannelTarget("wecom-app", "zhangsan", null, "lisi", Map.of()),
        "hello wecom",
        Map.of("safe", Integer.valueOf(1))));
    ChannelDeliveryResult secondResult = session.send(new ChannelOutboundRequest(
        new ChannelTarget("wecom-app", "wangwu", null, null, Map.of()),
        "follow up",
        Map.of()));

    Assertions.assertEquals(1, apiClient.tokenRequests);
    Assertions.assertEquals("corp-id", apiClient.accessTokenCommand.corpId());
    Assertions.assertEquals("corp-secret", apiClient.accessTokenCommand.corpSecret());
    Assertions.assertEquals("wecom-token", apiClient.firstSendCommand.accessToken());
    Assertions.assertEquals("1000002", apiClient.firstSendCommand.agentId());
    Assertions.assertEquals("lisi", apiClient.firstSendCommand.toUser());
    Assertions.assertEquals("hello wecom", apiClient.firstSendCommand.text());
    Assertions.assertEquals(1, apiClient.firstSendCommand.safe());
    Assertions.assertEquals("wangwu", apiClient.secondSendCommand.toUser());
    Assertions.assertEquals("follow up", apiClient.secondSendCommand.text());
    Assertions.assertEquals("wecom:msg-1", firstResult.deliveryId());
    Assertions.assertEquals("wecom:msg-2", secondResult.deliveryId());
  }

  @Test
  void shouldRejectMissingCorporateCredentials() {
    WeComChannelDriver driver = new WeComChannelDriver(new RecordingWeComApplicationApiClient());

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> driver.openSession(new ChannelAccountProfile(
            "wecom-app",
            ChannelType.WECOM,
            "WeCom App",
            Map.of("corpId", "corp-id", "agentId", "1000002"))));
  }

  private static final class RecordingWeComApplicationApiClient implements WeComApplicationApiClient {
    private int tokenRequests;
    private WeComAccessTokenCommand accessTokenCommand;
    private WeComSendCommand firstSendCommand;
    private WeComSendCommand secondSendCommand;
    private int sendRequests;

    @Override
    public WeComAccessTokenResult fetchAccessToken(WeComAccessTokenCommand command) {
      tokenRequests += 1;
      accessTokenCommand = command;
      return new WeComAccessTokenResult("wecom-token", Instant.now().plusSeconds(300));
    }

    @Override
    public WeComSendResult sendText(WeComSendCommand command) {
      sendRequests += 1;
      if (sendRequests == 1) {
        firstSendCommand = command;
        return new WeComSendResult("msg-1", Instant.ofEpochSecond(1_700_000_100L));
      }
      secondSendCommand = command;
      return new WeComSendResult("msg-2", Instant.ofEpochSecond(1_700_000_200L));
    }
  }
}
