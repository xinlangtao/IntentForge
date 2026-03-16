package cn.intentforge.channel.telegram;

import static cn.intentforge.common.util.ValidationSupport.normalize;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelInboundMessage;
import cn.intentforge.channel.ChannelParticipant;
import cn.intentforge.channel.ChannelTarget;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookHandler;
import cn.intentforge.channel.ChannelWebhookRequest;
import cn.intentforge.channel.ChannelWebhookResponse;
import cn.intentforge.channel.ChannelWebhookResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Normalizes Telegram webhook updates into shared channel inbound messages.
 *
 * @since 1.0.0
 */
final class TelegramWebhookHandler implements ChannelWebhookHandler {
  private static final ChannelWebhookResponse OK_RESPONSE =
      new ChannelWebhookResponse(200, "text/plain; charset=utf-8", "OK", Map.of());

  private final ChannelAccountProfile accountProfile;
  private final ObjectMapper objectMapper;

  TelegramWebhookHandler(ChannelAccountProfile accountProfile) {
    this(accountProfile, new ObjectMapper());
  }

  TelegramWebhookHandler(ChannelAccountProfile accountProfile, ObjectMapper objectMapper) {
    this.accountProfile = Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public ChannelWebhookResult handle(ChannelWebhookRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    if (!"POST".equals(request.method())) {
      return new ChannelWebhookResult(
          List.of(),
          new ChannelWebhookResponse(
              405,
              "text/plain; charset=utf-8",
              "method not allowed",
              Map.of("Allow", "POST")));
    }
    JsonNode root = readPayload(requireText(request.body(), "body"));
    JsonNode message = firstMessageNode(root);
    if (message == null) {
      return new ChannelWebhookResult(List.of(), OK_RESPONSE);
    }
    String text = textValue(message.get("text"));
    if (text == null) {
      return new ChannelWebhookResult(List.of(), OK_RESPONSE);
    }
    JsonNode chat = Objects.requireNonNull(message.get("chat"), "chat must not be null");
    JsonNode sender = Objects.requireNonNull(message.get("from"), "from must not be null");
    String chatId = requireText(textValue(chat.get("id")), "chat.id");
    String inboundMessageId = requireText(textValue(message.get("message_id")), "message.message_id");
    Map<String, String> targetAttributes = new LinkedHashMap<>();
    putIfPresent(targetAttributes, "chatType", textValue(chat.get("type")));
    putIfPresent(targetAttributes, "chatTitle", textValue(chat.get("title")));
    Map<String, String> senderAttributes = new LinkedHashMap<>();
    putIfPresent(senderAttributes, "username", textValue(sender.get("username")));
    putIfPresent(senderAttributes, "languageCode", textValue(sender.get("language_code")));
    Map<String, Object> metadata = new LinkedHashMap<>();
    putIfPresent(metadata, "updateId", longValue(root.get("update_id")));
    putIfPresent(metadata, "chatId", chatId);
    putIfPresent(metadata, "messageId", inboundMessageId);
    putIfPresent(metadata, "chatType", textValue(chat.get("type")));
    putIfPresent(metadata, "messageCreatedAt", instantValue(message.get("date")));
    return new ChannelWebhookResult(
        List.of(new ChannelInboundMessage(
            chatId + ":" + inboundMessageId,
            accountProfile.id(),
            ChannelType.TELEGRAM,
            new ChannelTarget(
                accountProfile.id(),
                chatId,
                textValue(message.get("message_thread_id")),
                null,
                Map.copyOf(targetAttributes)),
            new ChannelParticipant(
                requireText(textValue(sender.get("id")), "from.id"),
                displayName(sender),
                booleanValue(sender.get("is_bot")),
                Map.copyOf(senderAttributes)),
            text,
            Map.copyOf(metadata))),
        OK_RESPONSE);
  }

  private JsonNode readPayload(String body) {
    try {
      return objectMapper.readTree(body);
    } catch (IOException ex) {
      throw new IllegalArgumentException("invalid Telegram webhook payload", ex);
    }
  }

  private static JsonNode firstMessageNode(JsonNode root) {
    if (root == null) {
      return null;
    }
    for (String fieldName : List.of("message", "edited_message", "channel_post", "edited_channel_post")) {
      JsonNode candidate = root.get(fieldName);
      if (candidate != null && !candidate.isNull()) {
        return candidate;
      }
    }
    return null;
  }

  private static String displayName(JsonNode sender) {
    String firstName = textValue(sender.get("first_name"));
    String lastName = textValue(sender.get("last_name"));
    String username = textValue(sender.get("username"));
    StringBuilder displayName = new StringBuilder();
    if (firstName != null) {
      displayName.append(firstName);
    }
    if (lastName != null) {
      if (displayName.length() > 0) {
        displayName.append(' ');
      }
      displayName.append(lastName);
    }
    String normalizedDisplayName = normalize(displayName.toString());
    return normalizedDisplayName == null ? username : normalizedDisplayName;
  }

  private static String textValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    return normalize(node.asText());
  }

  private static Long longValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    return node.canConvertToLong() ? node.longValue() : Long.valueOf(node.asText());
  }

  private static Instant instantValue(JsonNode node) {
    Long epochSeconds = longValue(node);
    return epochSeconds == null ? null : Instant.ofEpochSecond(epochSeconds);
  }

  private static boolean booleanValue(JsonNode node) {
    return node != null && !node.isNull() && node.asBoolean(false);
  }

  private static void putIfPresent(Map<String, String> target, String key, String value) {
    String normalizedValue = normalize(value);
    if (normalizedValue != null) {
      target.put(key, normalizedValue);
    }
  }

  private static void putIfPresent(Map<String, Object> target, String key, Object value) {
    if (value != null) {
      target.put(key, value);
    }
  }
}
