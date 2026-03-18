package cn.intentforge.channel.wecom.inbound.callback;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelWebhookHandler;
import cn.intentforge.channel.ChannelWebhookRequest;
import cn.intentforge.channel.ChannelWebhookResponse;
import cn.intentforge.channel.ChannelWebhookResult;
import cn.intentforge.channel.wecom.crypto.WeComJsonCryptor;
import cn.intentforge.channel.wecom.inbound.common.WeComRobotInboundNormalizer;
import cn.intentforge.channel.wecom.shared.WeComJsonSupport;
import cn.intentforge.channel.wecom.shared.WeComPropertyNames;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * WeCom intelligent-robot callback handler.
 *
 * @since 1.0.0
 */
public final class WeComWebhookHandler implements ChannelWebhookHandler {
  private static final ChannelWebhookResponse SUCCESS_RESPONSE =
      new ChannelWebhookResponse(200, "text/plain; charset=utf-8", "success", Map.of());

  private final ChannelAccountProfile accountProfile;
  private final WeComJsonCryptor cryptor;
  private final WeComRobotInboundNormalizer normalizer;

  /**
   * Creates one callback handler bound to the provided WeCom account.
   *
   * @param accountProfile WeCom account profile
   */
  public WeComWebhookHandler(ChannelAccountProfile accountProfile) {
    this(
        Objects.requireNonNull(accountProfile, "accountProfile must not be null"),
        new WeComJsonCryptor(
            requireText(accountProfile.properties().get(WeComPropertyNames.CALLBACK_TOKEN), WeComPropertyNames.CALLBACK_TOKEN),
            requireText(
                accountProfile.properties().get(WeComPropertyNames.CALLBACK_ENCODING_AES_KEY),
                WeComPropertyNames.CALLBACK_ENCODING_AES_KEY),
            accountProfile.properties().get(WeComPropertyNames.RECEIVE_ID)),
        new WeComRobotInboundNormalizer());
  }

  WeComWebhookHandler(
      ChannelAccountProfile accountProfile,
      WeComJsonCryptor cryptor,
      WeComRobotInboundNormalizer normalizer
  ) {
    this.accountProfile = Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    this.cryptor = Objects.requireNonNull(cryptor, "cryptor must not be null");
    this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
  }

  @Override
  public ChannelWebhookResult handle(ChannelWebhookRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    return switch (request.method()) {
      case "GET" -> handleVerification(request);
      case "POST" -> handleCallback(request);
      default -> new ChannelWebhookResult(
          List.of(),
          new ChannelWebhookResponse(
              405,
              "text/plain; charset=utf-8",
              "method not allowed",
              Map.of("Allow", "GET, POST")));
    };
  }

  private ChannelWebhookResult handleVerification(ChannelWebhookRequest request) {
    String decrypted = cryptor.verifyAndDecrypt(
        requireText(request.firstQueryParameter("msg_signature"), "msg_signature"),
        requireText(request.firstQueryParameter("timestamp"), "timestamp"),
        requireText(request.firstQueryParameter("nonce"), "nonce"),
        requireText(request.firstQueryParameter("echostr"), "echostr"));
    return new ChannelWebhookResult(
        List.of(),
        new ChannelWebhookResponse(200, "text/plain; charset=utf-8", decrypted, Map.of()));
  }

  private ChannelWebhookResult handleCallback(ChannelWebhookRequest request) {
    JsonNode root = WeComJsonSupport.readTree(requireText(request.body(), "body"), "invalid WeCom callback body");
    String encrypted = encryptedPayload(root);
    String decrypted = cryptor.verifyAndDecrypt(
        requireText(request.firstQueryParameter("msg_signature"), "msg_signature"),
        requireText(request.firstQueryParameter("timestamp"), "timestamp"),
        requireText(request.firstQueryParameter("nonce"), "nonce"),
        encrypted);
    return new ChannelWebhookResult(
        normalizer.normalize(accountProfile, decrypted),
        SUCCESS_RESPONSE);
  }

  private static String encryptedPayload(JsonNode root) {
    String encrypted = text(root, "encrypt");
    if (encrypted != null) {
      return encrypted;
    }
    return requireText(text(root, "Encrypt"), "encrypt");
  }

  private static String text(JsonNode root, String fieldName) {
    if (root == null || root.isNull()) {
      return null;
    }
    JsonNode node = root.get(fieldName);
    if (node == null || node.isNull()) {
      return null;
    }
    return cn.intentforge.common.util.ValidationSupport.normalize(node.asText());
  }
}
