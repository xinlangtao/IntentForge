package cn.intentforge.channel.wecom;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelInboundMessage;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookHandler;
import cn.intentforge.channel.ChannelWebhookRequest;
import cn.intentforge.channel.ChannelWebhookResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WeComInboundWebhookHandlerTest {
  @Test
  void shouldParseTextCallbackIntoChannelInboundMessage() {
    WeComChannelDriver driver = new WeComChannelDriver(new NoOpWeComApplicationApiClient());
    ChannelAccountProfile accountProfile = new ChannelAccountProfile(
        "wecom-app",
        ChannelType.WECOM,
        "WeCom App",
        Map.of(
            "corpId", "corp-id",
            "agentId", "1000002",
            "corpSecret", "corp-secret"));

    ChannelWebhookHandler webhookHandler = driver.openWebhookHandler(accountProfile).orElseThrow();
    ChannelWebhookResult result = webhookHandler.handle(new ChannelWebhookRequest(
        "POST",
        Map.of("Content-Type", List.of("application/xml")),
        Map.of(),
        """
            <xml>
              <ToUserName><![CDATA[ww1234567890]]></ToUserName>
              <FromUserName><![CDATA[zhangsan]]></FromUserName>
              <CreateTime>1700000100</CreateTime>
              <MsgType><![CDATA[text]]></MsgType>
              <Content><![CDATA[hello wecom]]></Content>
              <MsgId>1234567890123456</MsgId>
              <AgentID>1000002</AgentID>
            </xml>
            """));

    Assertions.assertEquals(200, result.response().statusCode());
    Assertions.assertEquals("success", result.response().body());
    Assertions.assertEquals(1, result.messages().size());
    ChannelInboundMessage message = result.messages().getFirst();
    Assertions.assertEquals("1234567890123456", message.messageId());
    Assertions.assertEquals("wecom-app", message.accountId());
    Assertions.assertEquals(ChannelType.WECOM, message.type());
    Assertions.assertEquals("zhangsan", message.target().conversationId());
    Assertions.assertEquals("zhangsan", message.target().recipientId());
    Assertions.assertEquals("zhangsan", message.sender().id());
    Assertions.assertEquals("hello wecom", message.text());
    Assertions.assertEquals(Instant.ofEpochSecond(1_700_000_100L), message.metadata().get("messageCreatedAt"));
    Assertions.assertEquals("1000002", message.metadata().get("agentId"));
  }

  @Test
  void shouldEchoVerificationChallenge() {
    WeComChannelDriver driver = new WeComChannelDriver(new NoOpWeComApplicationApiClient());
    ChannelAccountProfile accountProfile = new ChannelAccountProfile(
        "wecom-app",
        ChannelType.WECOM,
        "WeCom App",
        Map.of(
            "corpId", "corp-id",
            "agentId", "1000002",
            "corpSecret", "corp-secret"));

    ChannelWebhookHandler webhookHandler = driver.openWebhookHandler(accountProfile).orElseThrow();
    ChannelWebhookResult result = webhookHandler.handle(new ChannelWebhookRequest(
        "GET",
        Map.of(),
        Map.of("echostr", List.of("verify-me")),
        ""));

    Assertions.assertEquals(200, result.response().statusCode());
    Assertions.assertEquals("verify-me", result.response().body());
    Assertions.assertTrue(result.messages().isEmpty());
  }

  @Test
  void shouldRejectMalformedCallbackPayload() {
    WeComChannelDriver driver = new WeComChannelDriver(new NoOpWeComApplicationApiClient());
    ChannelAccountProfile accountProfile = new ChannelAccountProfile(
        "wecom-app",
        ChannelType.WECOM,
        "WeCom App",
        Map.of(
            "corpId", "corp-id",
            "agentId", "1000002",
            "corpSecret", "corp-secret"));

    ChannelWebhookHandler webhookHandler = driver.openWebhookHandler(accountProfile).orElseThrow();

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> webhookHandler.handle(new ChannelWebhookRequest("POST", Map.of(), Map.of(), "<xml>")));
  }

  private static final class NoOpWeComApplicationApiClient implements WeComApplicationApiClient {
    @Override
    public WeComAccessTokenResult fetchAccessToken(WeComAccessTokenCommand command) {
      return new WeComAccessTokenResult("unused", Instant.now().plusSeconds(300));
    }

    @Override
    public WeComSendResult sendText(WeComSendCommand command) {
      return new WeComSendResult("unused", Instant.EPOCH);
    }
  }
}
