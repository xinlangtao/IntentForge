package cn.intentforge.channel.telegram.inbound.polling;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class HttpTelegramLongPollingApiClient implements TelegramLongPollingApiClient {
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  HttpTelegramLongPollingApiClient() {
    this(
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
        new ObjectMapper());
  }

  HttpTelegramLongPollingApiClient(HttpClient httpClient, ObjectMapper objectMapper) {
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public List<TelegramFetchedUpdate> getUpdates(TelegramGetUpdatesCommand command) {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(command.baseUrl() + "/bot" + command.botToken() + "/getUpdates"))
          .timeout(Duration.ofSeconds(Math.max(5, command.timeoutSeconds() + 5L)))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody(command)), StandardCharsets.UTF_8))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      JsonNode root = objectMapper.readTree(response.body());
      if (response.statusCode() < 200 || response.statusCode() >= 300 || !root.path("ok").asBoolean(false)) {
        throw new IllegalStateException("telegram getUpdates failed: " + readOptionalText(root, "description"));
      }
      List<TelegramFetchedUpdate> updates = new ArrayList<>();
      for (JsonNode update : root.path("result")) {
        long updateId = update.path("update_id").asLong(Long.MIN_VALUE);
        if (updateId == Long.MIN_VALUE) {
          throw new IllegalStateException("telegram getUpdates returned an update without update_id");
        }
        updates.add(new TelegramFetchedUpdate(updateId, objectMapper.writeValueAsString(update)));
      }
      return List.copyOf(updates);
    } catch (IOException exception) {
      throw new IllegalStateException("telegram getUpdates failed to serialize or parse payload", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("telegram getUpdates interrupted", exception);
    }
  }

  private static Map<String, Object> requestBody(TelegramGetUpdatesCommand command) {
    Map<String, Object> body = new LinkedHashMap<>();
    if (command.offset() != null) {
      body.put("offset", command.offset());
    }
    body.put("limit", command.limit());
    body.put("timeout", command.timeoutSeconds());
    if (!command.allowedUpdates().isEmpty()) {
      body.put("allowed_updates", command.allowedUpdates());
    }
    return body;
  }

  private static String readOptionalText(JsonNode node, String fieldName) {
    if (node == null || node.isMissingNode() || node.path(fieldName).isMissingNode() || node.path(fieldName).isNull()) {
      return null;
    }
    return requireText(node.path(fieldName).asText(), fieldName);
  }
}
