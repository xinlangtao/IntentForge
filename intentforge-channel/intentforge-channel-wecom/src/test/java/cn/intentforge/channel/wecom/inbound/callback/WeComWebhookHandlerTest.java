package cn.intentforge.channel.wecom.inbound.callback;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelInboundMessage;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookRequest;
import cn.intentforge.channel.ChannelWebhookResult;
import cn.intentforge.channel.wecom.crypto.WeComEncryptedResponse;
import cn.intentforge.channel.wecom.crypto.WeComJsonCryptor;
import cn.intentforge.channel.wecom.shared.WeComPropertyNames;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WeComWebhookHandlerTest {
  private static final String TOKEN = "robot-token";
  private static final String ENCODING_AES_KEY = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";

  @Test
  void shouldDecryptAndNormalizeTextCallback() {
    ChannelAccountProfile accountProfile = accountProfile();
    WeComJsonCryptor cryptor = new WeComJsonCryptor(TOKEN, ENCODING_AES_KEY, "robot-receive-id");
    WeComEncryptedResponse encrypted = cryptor.encrypt(
        """
            {
              "msgid":"msg-1",
              "msgtype":"text",
              "robotid":"robot-123",
              "chatid":"chat-1",
              "from":{"userid":"zhangsan"},
              "text":{"content":"hello wecom robot"},
              "msgtime":1700000100
            }
            """,
        "1710000000",
        "nonce-1");

    WeComWebhookHandler handler = new WeComWebhookHandler(accountProfile);
    ChannelWebhookResult result = handler.handle(new ChannelWebhookRequest(
        "POST",
        Map.of("Content-Type", List.of("application/json")),
        Map.of(
            "msg_signature", List.of(encrypted.messageSignature()),
            "timestamp", List.of(encrypted.timestamp()),
            "nonce", List.of(encrypted.nonce())),
        "{\"encrypt\":\"" + encrypted.encrypt() + "\"}"));

    Assertions.assertEquals(200, result.response().statusCode());
    Assertions.assertEquals("success", result.response().body());
    Assertions.assertEquals(1, result.messages().size());
    ChannelInboundMessage message = result.messages().getFirst();
    Assertions.assertEquals("msg-1", message.messageId());
    Assertions.assertEquals("wecom-robot", message.accountId());
    Assertions.assertEquals(ChannelType.WECOM, message.type());
    Assertions.assertEquals("chat-1", message.target().conversationId());
    Assertions.assertEquals("zhangsan", message.target().recipientId());
    Assertions.assertEquals("zhangsan", message.sender().id());
    Assertions.assertEquals("hello wecom robot", message.text());
    Assertions.assertEquals("robot-123", message.metadata().get("robotId"));
    Assertions.assertEquals(Instant.ofEpochSecond(1_700_000_100L), message.metadata().get("messageCreatedAt"));
  }

  @Test
  void shouldDecryptVerificationChallenge() {
    ChannelAccountProfile accountProfile = accountProfile();
    WeComJsonCryptor cryptor = new WeComJsonCryptor(TOKEN, ENCODING_AES_KEY, "robot-receive-id");
    WeComEncryptedResponse encrypted = cryptor.encrypt("verify-me", "1710000001", "nonce-2");
    WeComWebhookHandler handler = new WeComWebhookHandler(accountProfile);

    ChannelWebhookResult result = handler.handle(new ChannelWebhookRequest(
        "GET",
        Map.of(),
        Map.of(
            "msg_signature", List.of(encrypted.messageSignature()),
            "timestamp", List.of(encrypted.timestamp()),
            "nonce", List.of(encrypted.nonce()),
            "echostr", List.of(encrypted.encrypt())),
        ""));

    Assertions.assertEquals(200, result.response().statusCode());
    Assertions.assertEquals("verify-me", result.response().body());
    Assertions.assertTrue(result.messages().isEmpty());
  }

  @Test
  void shouldRejectMissingSignatureParameters() {
    WeComWebhookHandler handler = new WeComWebhookHandler(accountProfile());

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> handler.handle(new ChannelWebhookRequest(
            "POST",
            Map.of(),
            Map.of(),
            "{\"encrypt\":\"cipher\"}")));
  }

  private static ChannelAccountProfile accountProfile() {
    return new ChannelAccountProfile(
        "wecom-robot",
        ChannelType.WECOM,
        "WeCom Robot",
        Map.of(
            WeComPropertyNames.CALLBACK_TOKEN, TOKEN,
            WeComPropertyNames.CALLBACK_ENCODING_AES_KEY, ENCODING_AES_KEY,
            WeComPropertyNames.RECEIVE_ID, "robot-receive-id",
            WeComPropertyNames.ROBOT_ID, "robot-123",
            WeComPropertyNames.ROBOT_SECRET, "robot-secret"));
  }
}
