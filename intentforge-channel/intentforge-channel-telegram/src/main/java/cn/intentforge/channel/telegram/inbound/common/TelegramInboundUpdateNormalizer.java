package cn.intentforge.channel.telegram.inbound.common;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelInboundMessage;
import cn.intentforge.channel.ChannelParticipant;
import cn.intentforge.channel.ChannelTarget;
import cn.intentforge.channel.ChannelType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Normalizes Telegram update payloads into shared inbound channel messages.
 *
 * @since 1.0.0
 */
public final class TelegramInboundUpdateNormalizer {
  private final ObjectMapper objectMapper;

  /**
   * Creates one normalizer with the default JSON mapper.
   */
  public TelegramInboundUpdateNormalizer() {
    this(new ObjectMapper());
  }

  TelegramInboundUpdateNormalizer(ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  /**
   * Normalizes one raw Telegram update payload.
   *
   * @param accountProfile Telegram account profile
   * @param payload raw Telegram update payload
   * @return normalized inbound messages extracted from the payload
   */
  public List<ChannelInboundMessage> normalize(ChannelAccountProfile accountProfile, String payload) {
    Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    JsonNode root = readPayload(requireText(payload, "payload"));
    JsonNode callbackQuery = root.get("callback_query");
    if (callbackQuery != null && !callbackQuery.isNull()) {
      return callbackQueryMessages(accountProfile, root, callbackQuery);
    }
    JsonNode message = firstMessageNode(root);
    if (message == null) {
      return List.of();
    }
    return messageResult(accountProfile, root, message);
  }

  private List<ChannelInboundMessage> messageResult(ChannelAccountProfile accountProfile, JsonNode root, JsonNode message) {
    String text = textValue(message.get("text"));
    if (text == null) {
      return List.of();
    }
    JsonNode chat = Objects.requireNonNull(message.get("chat"), "chat must not be null");
    JsonNode sender = Objects.requireNonNull(message.get("from"), "from must not be null");
    String chatId = requireText(textValue(chat.get("id")), "chat.id");
    String inboundMessageId = requireText(textValue(message.get("message_id")), "message.message_id");
    Map<String, String> targetAttributes = new LinkedHashMap<>();
    putIfPresentString(targetAttributes, "chatType", textValue(chat.get("type")));
    putIfPresentString(targetAttributes, "chatTitle", textValue(chat.get("title")));
    Map<String, String> senderAttributes = new LinkedHashMap<>();
    putIfPresentString(senderAttributes, "username", textValue(sender.get("username")));
    putIfPresentString(senderAttributes, "languageCode", textValue(sender.get("language_code")));
    Map<String, Object> metadata = new LinkedHashMap<>();
    putIfPresentObject(metadata, "updateId", longValue(root.get("update_id")));
    putIfPresentObject(metadata, "updateKind", "message");
    putIfPresentObject(metadata, "chatId", chatId);
    putIfPresentObject(metadata, "messageId", inboundMessageId);
    putIfPresentObject(metadata, "chatType", textValue(chat.get("type")));
    putIfPresentObject(metadata, "messageCreatedAt", instantValue(message.get("date")));
    return List.of(new ChannelInboundMessage(
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
        Map.copyOf(metadata)));
  }

  private List<ChannelInboundMessage> callbackQueryMessages(
      ChannelAccountProfile accountProfile,
      JsonNode root,
      JsonNode callbackQuery
  ) {
    String callbackData = firstNonBlank(textValue(callbackQuery.get("data")), textValue(callbackQuery.get("game_short_name")));
    if (callbackData == null) {
      return List.of();
    }
    JsonNode message = callbackQuery.get("message");
    if (message == null || message.isNull()) {
      return List.of();
    }
    JsonNode chat = Objects.requireNonNull(message.get("chat"), "callback_query.message.chat must not be null");
    JsonNode sender = Objects.requireNonNull(callbackQuery.get("from"), "callback_query.from must not be null");
    String chatId = requireText(textValue(chat.get("id")), "callback_query.message.chat.id");
    String callbackQueryId = requireText(textValue(callbackQuery.get("id")), "callback_query.id");
    String originMessageId = requireText(textValue(message.get("message_id")), "callback_query.message.message_id");
    Map<String, String> targetAttributes = new LinkedHashMap<>();
    putIfPresentString(targetAttributes, "chatType", textValue(chat.get("type")));
    putIfPresentString(targetAttributes, "chatTitle", textValue(chat.get("title")));
    Map<String, String> senderAttributes = new LinkedHashMap<>();
    putIfPresentString(senderAttributes, "username", textValue(sender.get("username")));
    putIfPresentString(senderAttributes, "languageCode", textValue(sender.get("language_code")));
    Map<String, Object> metadata = new LinkedHashMap<>();
    putIfPresentObject(metadata, "updateId", longValue(root.get("update_id")));
    putIfPresentObject(metadata, "updateKind", "callback_query");
    putIfPresentObject(metadata, "callbackQueryId", callbackQueryId);
    putIfPresentObject(metadata, "callbackData", callbackData);
    putIfPresentObject(metadata, "chatId", chatId);
    putIfPresentObject(metadata, "messageId", originMessageId);
    putIfPresentObject(metadata, "chatType", textValue(chat.get("type")));
    putIfPresentObject(metadata, "messageCreatedAt", instantValue(message.get("date")));
    putIfPresentObject(metadata, "chatInstance", textValue(callbackQuery.get("chat_instance")));
    return List.of(new ChannelInboundMessage(
        "callback:" + callbackQueryId,
        accountProfile.id(),
        ChannelType.TELEGRAM,
        new ChannelTarget(
            accountProfile.id(),
            chatId,
            textValue(message.get("message_thread_id")),
            null,
            Map.copyOf(targetAttributes)),
        new ChannelParticipant(
            requireText(textValue(sender.get("id")), "callback_query.from.id"),
            displayName(sender),
            booleanValue(sender.get("is_bot")),
            Map.copyOf(senderAttributes)),
        callbackData,
        Map.copyOf(metadata)));
  }

  private JsonNode readPayload(String body) {
    try {
      return objectMapper.readTree(body);
    } catch (IOException exception) {
      throw new IllegalArgumentException("invalid Telegram webhook payload", exception);
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
    String normalizedDisplayName = cn.intentforge.common.util.ValidationSupport.normalize(displayName.toString());
    return normalizedDisplayName == null ? username : normalizedDisplayName;
  }

  private static String textValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    return cn.intentforge.common.util.ValidationSupport.normalize(node.asText());
  }

  private static String firstNonBlank(String preferred, String fallback) {
    String normalizedPreferred = cn.intentforge.common.util.ValidationSupport.normalize(preferred);
    return normalizedPreferred == null
        ? cn.intentforge.common.util.ValidationSupport.normalize(fallback)
        : normalizedPreferred;
  }

  private static Long longValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    return node.asLong();
  }

  private static Boolean booleanValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    return node.asBoolean();
  }

  private static Instant instantValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    return Instant.ofEpochSecond(node.asLong());
  }

  private static void putIfPresentString(Map<String, String> target, String key, String value) {
    String normalizedValue = cn.intentforge.common.util.ValidationSupport.normalize(value);
    if (normalizedValue != null) {
      target.put(key, normalizedValue);
    }
  }

  private static void putIfPresentObject(Map<String, Object> metadata, String key, Object value) {
    if (value == null) {
      return;
    }
    metadata.put(key, value);
  }
}
