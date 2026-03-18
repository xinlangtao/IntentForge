package cn.intentforge.boot.server;

import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.wecom.crypto.WeComEncryptedResponse;
import cn.intentforge.channel.wecom.crypto.WeComJsonCryptor;
import cn.intentforge.channel.wecom.shared.WeComPropertyNames;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WeComRobotServerMainTest {
  private static final String CALLBACK_TOKEN = "robot-token";
  private static final String CALLBACK_AES_KEY = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";

  @Test
  void shouldResolveSettingsFromSystemPropertiesBeforeEnvironment() {
    WeComRobotServerSettings settings = WeComRobotServerMain.resolveSettings(
        key -> switch (key) {
          case "intentforge.wecom.accountId" -> "wecom-demo";
          case "intentforge.wecom.displayName" -> "Demo Robot";
          case "intentforge.wecom.callbackToken" -> CALLBACK_TOKEN;
          case "intentforge.wecom.callbackEncodingAesKey" -> CALLBACK_AES_KEY;
          case "intentforge.wecom.receiveId" -> "receive-id";
          case "intentforge.wecom.robotId" -> "robot-123";
          default -> null;
        },
        key -> switch (key) {
          case "WECOM_ROBOT_SECRET" -> "env-robot-secret";
          case "WECOM_BASE_URL" -> "https://qyapi.example.com";
          default -> null;
        });

    Assertions.assertEquals("wecom-demo", settings.accountId());
    Assertions.assertEquals("Demo Robot", settings.displayName());
    Assertions.assertEquals(CALLBACK_TOKEN, settings.callbackToken());
    Assertions.assertEquals(CALLBACK_AES_KEY, settings.callbackEncodingAesKey());
    Assertions.assertEquals("receive-id", settings.receiveId());
    Assertions.assertEquals("robot-123", settings.robotId());
    Assertions.assertEquals("env-robot-secret", settings.robotSecret());
    Assertions.assertEquals("https://qyapi.example.com", settings.baseUrl());
  }

  @Test
  void shouldRejectMissingRequiredWeComRobotCredentials() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> WeComRobotServerMain.resolveSettings(
            key -> switch (key) {
              case "intentforge.wecom.callbackToken" -> CALLBACK_TOKEN;
              default -> null;
            },
            key -> null));
  }

  @Test
  void shouldStartServerWithWeComRobotHookAccountRegistration() throws Exception {
    Path pluginsDirectory = Files.createTempDirectory("wecom-robot-main-plugins");
    WeComRobotServerSettings settings = new WeComRobotServerSettings(
        "wecom-demo",
        "Demo Robot",
        CALLBACK_TOKEN,
        CALLBACK_AES_KEY,
        "robot-receive-id",
        "robot-123",
        "robot-secret",
        null);

    WeComJsonCryptor cryptor = new WeComJsonCryptor(
        settings.callbackToken(),
        settings.callbackEncodingAesKey(),
        settings.receiveId());
    WeComEncryptedResponse verification = cryptor.encrypt("verify-me", "1710000000", "nonce-1");
    WeComEncryptedResponse callback = cryptor.encrypt(
        """
            {
              "msgid":"msg-1",
              "msgtype":"text",
              "robotid":"robot-123",
              "chatid":"chat-1",
              "from":{"userid":"zhangsan"},
              "text":{"content":"hello wecom main"},
              "msgtime":1700000100
            }
            """,
        "1710000001",
        "nonce-2");

    try (AiAssetServerRuntime runtime = WeComRobotServerMain.startServer(
        new InetSocketAddress("127.0.0.1", 0),
        pluginsDirectory,
        settings)) {
      HttpClient client = HttpClient.newHttpClient();

      HttpResponse<String> verificationResponse = client.send(
          HttpRequest.newBuilder(runtime.baseUri().resolve(
                  "/open-api/hooks/wecom/accounts/wecom-demo/callback"
                      + "?msg_signature=" + verification.messageSignature()
                      + "&timestamp=" + verification.timestamp()
                      + "&nonce=" + verification.nonce()
                      + "&echostr=" + java.net.URLEncoder.encode(verification.encrypt(), StandardCharsets.UTF_8)))
              .timeout(Duration.ofSeconds(10))
              .GET()
              .build(),
          HttpResponse.BodyHandlers.ofString());

      HttpResponse<String> callbackResponse = client.send(
          HttpRequest.newBuilder(runtime.baseUri().resolve(
                  "/open-api/hooks/wecom/accounts/wecom-demo/callback"
                      + "?msg_signature=" + callback.messageSignature()
                      + "&timestamp=" + callback.timestamp()
                      + "&nonce=" + callback.nonce()))
              .timeout(Duration.ofSeconds(10))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString("{\"encrypt\":\"" + callback.encrypt() + "\"}"))
              .build(),
          HttpResponse.BodyHandlers.ofString());

      Assertions.assertEquals(200, verificationResponse.statusCode());
      Assertions.assertEquals("verify-me", verificationResponse.body());
      Assertions.assertEquals(200, callbackResponse.statusCode());
      Assertions.assertEquals("success", callbackResponse.body());
      Assertions.assertTrue(runtime.localRuntime().sessionManager().find("wecom:chat-1").isPresent());
      Assertions.assertTrue(runtime.localRuntime().channelManager().openWebhookHandler(settings.toAccountProfile()).isPresent());
      Assertions.assertEquals(ChannelType.WECOM, settings.toAccountProfile().type());
      Assertions.assertEquals(CALLBACK_TOKEN, settings.toAccountProfile().properties().get(WeComPropertyNames.CALLBACK_TOKEN));
      Assertions.assertEquals("robot-secret", settings.toAccountProfile().properties().get(WeComPropertyNames.ROBOT_SECRET));
    }
  }
}
