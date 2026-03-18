package cn.intentforge.channel.wecom.outbound;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import cn.intentforge.channel.wecom.shared.WeComJsonSupport;
import com.fasterxml.jackson.databind.JsonNode;
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
 * HTTP implementation of the WeCom intelligent-robot outbound API client.
 *
 * @since 1.0.0
 */
public final class HttpWeComRobotApiClient implements WeComRobotApiClient {
  private final HttpClient httpClient;

  /**
   * Creates one API client with the default HTTP client.
   */
  public HttpWeComRobotApiClient() {
    this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
  }

  /**
   * Creates one API client with the provided HTTP client.
   *
   * @param httpClient HTTP client
   */
  public HttpWeComRobotApiClient(HttpClient httpClient) {
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
  }

  @Override
  public WeComRobotSendMessageResult sendText(WeComRobotSendMessageCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(command.baseUrl() + "/cgi-bin/intelligent_robot/message/send"))
          .timeout(Duration.ofSeconds(20))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(
              WeComJsonSupport.writeValue(requestBody(command), "failed to serialize WeCom robot send request"),
              StandardCharsets.UTF_8))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      JsonNode root = WeComJsonSupport.readTree(response.body(), "invalid WeCom robot send response");
      ensureSuccess(response.statusCode(), root);
      return new WeComRobotSendMessageResult(
          firstNonBlank(text(root, "msgid"), text(root, "message_id")),
          Instant.now());
    } catch (IOException exception) {
      throw new IllegalStateException("wecom intelligent-robot send failed to serialize or parse payload", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("wecom intelligent-robot send interrupted", exception);
    }
  }

  private static Map<String, Object> requestBody(WeComRobotSendMessageCommand command) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("robot_id", command.robotId());
    body.put("secret", command.robotSecret());
    if (command.chatId() != null) {
      body.put("chatid", command.chatId());
    }
    if (command.userId() != null) {
      body.put("userid", command.userId());
    }
    if (command.sessionId() != null) {
      body.put("session_id", command.sessionId());
    }
    body.put("msgtype", "text");
    body.put("text", Map.of("content", command.text()));
    return body;
  }

  private static void ensureSuccess(int statusCode, JsonNode root) {
    int errorCode = root.path("errcode").asInt(-1);
    if (statusCode < 200 || statusCode >= 300 || errorCode != 0) {
      throw new IllegalStateException("wecom intelligent-robot send failed: " + text(root, "errmsg"));
    }
  }

  private static String text(JsonNode root, String fieldName) {
    if (root == null || root.isNull() || root.path(fieldName).isMissingNode() || root.path(fieldName).isNull()) {
      return null;
    }
    return cn.intentforge.common.util.ValidationSupport.normalize(root.path(fieldName).asText());
  }

  private static String firstNonBlank(String first, String second) {
    String normalizedFirst = cn.intentforge.common.util.ValidationSupport.normalize(first);
    return normalizedFirst == null
        ? requireText(second, "messageId")
        : normalizedFirst;
  }
}
