package cn.intentforge.channel.wecom.inbound.common;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelInboundMessage;
import cn.intentforge.channel.ChannelParticipant;
import cn.intentforge.channel.ChannelTarget;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.wecom.shared.WeComJsonSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Normalizes decrypted WeCom intelligent-robot callback payloads into shared inbound messages.
 *
 * @since 1.0.0
 */
public final class WeComRobotInboundNormalizer {
  /**
   * Normalizes one decrypted callback payload.
   *
   * @param accountProfile WeCom account profile
   * @param payload decrypted callback payload
   * @return normalized inbound messages extracted from the payload
   */
  public List<ChannelInboundMessage> normalize(ChannelAccountProfile accountProfile, String payload) {
    Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    JsonNode root = WeComJsonSupport.readTree(requireText(payload, "payload"), "invalid WeCom intelligent-robot payload");
    String messageType = normalizedText(root, "msgtype");
    if (!"text".equalsIgnoreCase(messageType)) {
      return List.of();
    }
    String content = firstNonBlank(
        normalizedText(root.path("text"), "content"),
        normalizedText(root, "content"));
    if (content == null) {
      return List.of();
    }
    String senderId = firstNonBlank(
        normalizedText(root.path("from"), "userid"),
        normalizedText(root, "userid"));
    String chatId = firstNonBlank(
        normalizedText(root, "chatid"),
        normalizedText(root.path("conversation"), "chatid"));
    String conversationId = firstNonBlank(chatId, senderId);
    String messageId = firstNonBlank(
        normalizedText(root, "msgid"),
        conversationId + ":" + messageTimestamp(root).getEpochSecond());
    String robotId = firstNonBlank(
        normalizedText(root, "robotid"),
        normalizedText(root, "robotId"));
    String sessionId = firstNonBlank(
        normalizedText(root, "sessionid"),
        normalizedText(root, "sessionId"));

    Map<String, String> targetAttributes = new LinkedHashMap<>();
    putIfPresent(targetAttributes, "chatId", chatId);
    putIfPresent(targetAttributes, "robotId", robotId);

    Map<String, Object> metadata = new LinkedHashMap<>();
    putIfPresent(metadata, "msgType", messageType.toLowerCase(java.util.Locale.ROOT));
    putIfPresent(metadata, "chatId", chatId);
    putIfPresent(metadata, "robotId", robotId);
    putIfPresent(metadata, "sessionId", sessionId);
    putIfPresent(metadata, "messageCreatedAt", messageTimestamp(root));

    return List.of(new ChannelInboundMessage(
        messageId,
        accountProfile.id(),
        ChannelType.WECOM,
        new ChannelTarget(accountProfile.id(), conversationId, null, senderId, Map.copyOf(targetAttributes)),
        new ChannelParticipant(requireText(senderId, "senderId"), null, false, Map.of()),
        content,
        Map.copyOf(metadata)));
  }

  private static Instant messageTimestamp(JsonNode root) {
    JsonNode node = root.get("msgtime");
    if (node == null || node.isNull()) {
      return Instant.now();
    }
    long raw = node.asLong();
    if (raw >= 1_000_000_000_000L) {
      return Instant.ofEpochMilli(raw);
    }
    return Instant.ofEpochSecond(raw);
  }

  private static String normalizedText(JsonNode node, String fieldName) {
    if (node == null || node.isNull()) {
      return null;
    }
    JsonNode child = node.get(fieldName);
    if (child == null || child.isNull()) {
      return null;
    }
    return cn.intentforge.common.util.ValidationSupport.normalize(child.asText());
  }

  private static String firstNonBlank(String first, String second) {
    String normalizedFirst = cn.intentforge.common.util.ValidationSupport.normalize(first);
    return normalizedFirst == null
        ? cn.intentforge.common.util.ValidationSupport.normalize(second)
        : normalizedFirst;
  }

  private static void putIfPresent(Map<String, String> target, String key, String value) {
    if (value != null) {
      target.put(key, value);
    }
  }

  private static void putIfPresent(Map<String, Object> target, String key, Object value) {
    if (value != null) {
      target.put(key, value);
    }
  }
}
