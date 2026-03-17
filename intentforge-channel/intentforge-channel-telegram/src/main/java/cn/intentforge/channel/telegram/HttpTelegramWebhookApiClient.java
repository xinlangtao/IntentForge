package cn.intentforge.channel.telegram;

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
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class HttpTelegramWebhookApiClient implements TelegramWebhookApiClient {
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  HttpTelegramWebhookApiClient() {
    this(
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
        new ObjectMapper());
  }

  HttpTelegramWebhookApiClient(HttpClient httpClient, ObjectMapper objectMapper) {
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public void setWebhook(TelegramSetWebhookCommand command) {
    JsonNode root = invokeJson(
        command.baseUrl(),
        command.botToken(),
        "/setWebhook",
        "POST",
        requestBody(command));
    if (!root.path("ok").asBoolean(false)) {
      throw new IllegalStateException("telegram setWebhook failed: " + readOptionalText(root, "description"));
    }
  }

  @Override
  public void deleteWebhook(TelegramDeleteWebhookCommand command) {
    JsonNode root = invokeJson(
        command.baseUrl(),
        command.botToken(),
        "/deleteWebhook",
        "POST",
        Map.of("drop_pending_updates", command.dropPendingUpdates()));
    if (!root.path("ok").asBoolean(false)) {
      throw new IllegalStateException("telegram deleteWebhook failed: " + readOptionalText(root, "description"));
    }
  }

  @Override
  public TelegramWebhookInfo getWebhookInfo(TelegramWebhookInfoCommand command) {
    JsonNode root = invokeJson(command.baseUrl(), command.botToken(), "/getWebhookInfo", "GET", null);
    if (!root.path("ok").asBoolean(false)) {
      throw new IllegalStateException("telegram getWebhookInfo failed: " + readOptionalText(root, "description"));
    }
    JsonNode result = root.path("result");
    return new TelegramWebhookInfo(
        URI.create(requireText(readText(result, "url"), "result.url")),
        integerValue(result.get("pending_update_count")),
        integerValue(result.get("max_connections")),
        readOptionalText(result, "ip_address"),
        instantValue(result.get("last_error_date")),
        readOptionalText(result, "last_error_message"),
        textList(result.get("allowed_updates")));
  }

  private JsonNode invokeJson(
      String baseUrl,
      String botToken,
      String methodPath,
      String httpMethod,
      Map<String, Object> body
  ) {
    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + "/bot" + botToken + methodPath))
          .timeout(Duration.ofSeconds(20))
          .header("Content-Type", "application/json");
      if ("GET".equals(httpMethod)) {
        builder.GET();
      } else {
        builder.POST(HttpRequest.BodyPublishers.ofString(
            objectMapper.writeValueAsString(body == null ? Map.of() : body),
            StandardCharsets.UTF_8));
      }
      HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException("telegram webhook api failed with HTTP " + response.statusCode());
      }
      return objectMapper.readTree(response.body());
    } catch (IOException exception) {
      throw new IllegalStateException("telegram webhook api failed to serialize or parse payload", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("telegram webhook api interrupted", exception);
    }
  }

  private static Map<String, Object> requestBody(TelegramSetWebhookCommand command) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("url", command.webhookUrl().toString());
    if (command.secretToken() != null && !command.secretToken().isBlank()) {
      body.put("secret_token", command.secretToken());
    }
    if (!command.allowedUpdates().isEmpty()) {
      body.put("allowed_updates", command.allowedUpdates());
    }
    if (command.maxConnections() != null) {
      body.put("max_connections", command.maxConnections());
    }
    if (command.dropPendingUpdates()) {
      body.put("drop_pending_updates", Boolean.TRUE);
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

  private static Integer integerValue(JsonNode node) {
    return node == null || node.isNull() ? null : node.asInt();
  }

  private static Instant instantValue(JsonNode node) {
    return node == null || node.isNull() ? null : Instant.ofEpochSecond(node.asLong());
  }

  private static List<String> textList(JsonNode node) {
    if (node == null || node.isNull() || !node.isArray()) {
      return List.of();
    }
    List<String> values = new java.util.ArrayList<>();
    for (JsonNode child : node) {
      values.add(requireText(child.asText(), "allowed_updates item"));
    }
    return List.copyOf(values);
  }
}
