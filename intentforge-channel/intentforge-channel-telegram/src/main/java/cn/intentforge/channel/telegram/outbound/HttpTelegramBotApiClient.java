package cn.intentforge.channel.telegram.outbound;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP-based Telegram Bot API client for outbound message delivery.
 *
 * @since 1.0.0
 */
public final class HttpTelegramBotApiClient implements TelegramBotApiClient {
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  /**
   * Creates one client with the default HTTP and JSON support.
   */
  public HttpTelegramBotApiClient() {
    this(
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
        new ObjectMapper());
  }

  HttpTelegramBotApiClient(HttpClient httpClient, ObjectMapper objectMapper) {
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public TelegramSendResult sendMessage(TelegramSendCommand command) {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(command.baseUrl() + "/bot" + command.botToken() + "/sendMessage"))
          .timeout(Duration.ofSeconds(20))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody(command)), StandardCharsets.UTF_8))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      JsonNode root = objectMapper.readTree(response.body());
      if (response.statusCode() < 200 || response.statusCode() >= 300 || !root.path("ok").asBoolean(false)) {
        throw new IllegalStateException("telegram sendMessage failed: " + readOptionalText(root, "description"));
      }
      JsonNode result = root.path("result");
      return new TelegramSendResult(
          readText(result, "message_id"),
          Instant.ofEpochSecond(result.path("date").asLong(Instant.now().getEpochSecond())));
    } catch (IOException exception) {
      throw new IllegalStateException("telegram sendMessage failed to serialize or parse payload", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("telegram sendMessage interrupted", exception);
    }
  }

  private static Map<String, Object> requestBody(TelegramSendCommand command) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("chat_id", command.chatId());
    body.put("text", command.text());
    if (command.messageThreadId() != null) {
      body.put("message_thread_id", Long.parseLong(command.messageThreadId()));
    }
    if (command.parseMode() != null) {
      body.put("parse_mode", command.parseMode());
    }
    if (command.disableNotification()) {
      body.put("disable_notification", Boolean.TRUE);
    }
    if (command.disableWebPagePreview()) {
      body.put("link_preview_options", Map.of("is_disabled", Boolean.TRUE));
    }
    return body;
  }

  private static String readText(JsonNode node, String fieldName) {
    return requireText(readOptionalText(node, fieldName), fieldName);
  }

  private static String readOptionalText(JsonNode node, String fieldName) {
    if (node == null || node.isMissingNode() || node.path(fieldName).isMissingNode() || node.path(fieldName).isNull()) {
      return null;
    }
    return node.path(fieldName).asText();
  }
}
