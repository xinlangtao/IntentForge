package cn.intentforge.boot.local;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelInboundDispatch;
import cn.intentforge.channel.ChannelInboundProcessingResult;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookRequest;
import java.nio.file.Files;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AiAssetLocalBootstrapChannelInboundIntegrationTest {
  @Test
  void shouldExposeInboundProcessorAndApplyFallbackRouteResolution() throws Exception {
    AiAssetLocalRuntime runtime = AiAssetLocalBootstrap.bootstrap(Files.createTempDirectory("boot-channel-inbound-plugins"));
    ChannelAccountProfile accountProfile = new ChannelAccountProfile(
        "telegram-account",
        ChannelType.TELEGRAM,
        "Telegram Bot",
        Map.of("botToken", "bot-token"));

    ChannelInboundProcessingResult result = runtime.channelInboundProcessor().process(accountProfile, new ChannelWebhookRequest(
        "POST",
        Map.of(),
        Map.of(),
        """
            {
              "update_id": 9001,
              "message": {
                "message_id": 42,
                "text": "hello inbound",
                "chat": {
                  "id": -100123,
                  "type": "private"
                },
                "from": {
                  "id": 99,
                  "is_bot": false,
                  "first_name": "Ada"
                }
              }
            }
            """));

    Assertions.assertEquals(200, result.response().statusCode());
    Assertions.assertEquals(1, result.dispatches().size());
    ChannelInboundDispatch dispatch = result.dispatches().getFirst();
    Assertions.assertTrue(dispatch.accessDecision().allowed());
    Assertions.assertEquals("telegram-account", dispatch.routeDecision().orElseThrow().spaceId());
    Assertions.assertEquals("telegram:-100123", dispatch.routeDecision().orElseThrow().sessionId());
  }
}
