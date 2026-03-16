package cn.intentforge.channel.connectors.wecom;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class HttpWeComApplicationApiClient implements WeComApplicationApiClient {
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  HttpWeComApplicationApiClient() {
    this(
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
        new ObjectMapper());
  }

  HttpWeComApplicationApiClient(HttpClient httpClient, ObjectMapper objectMapper) {
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public WeComAccessTokenResult fetchAccessToken(WeComAccessTokenCommand command) {
    try {
      String uri = command.baseUrl()
          + "/cgi-bin/gettoken?corpid=" + encode(command.corpId())
          + "&corpsecret=" + encode(command.corpSecret());
      HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
          .timeout(Duration.ofSeconds(20))
          .GET()
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      JsonNode root = objectMapper.readTree(response.body());
      ensureSuccess(response.statusCode(), root, "wecom gettoken");
      return new WeComAccessTokenResult(
          readText(root, "access_token"),
          Instant.now().plusSeconds(root.path("expires_in").asLong(7200L)));
    } catch (IOException exception) {
      throw new IllegalStateException("wecom gettoken failed to serialize or parse payload", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("wecom gettoken interrupted", exception);
    }
  }

  @Override
  public WeComSendResult sendText(WeComSendCommand command) {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(
              command.baseUrl() + "/cgi-bin/message/send?access_token=" + encode(command.accessToken())))
          .timeout(Duration.ofSeconds(20))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody(command)), StandardCharsets.UTF_8))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      JsonNode root = objectMapper.readTree(response.body());
      ensureSuccess(response.statusCode(), root, "wecom message/send");
      return new WeComSendResult(
          readText(root, "msgid"),
          Instant.now());
    } catch (IOException exception) {
      throw new IllegalStateException("wecom message/send failed to serialize or parse payload", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("wecom message/send interrupted", exception);
    }
  }

  private static Map<String, Object> requestBody(WeComSendCommand command) {
    Map<String, Object> body = new LinkedHashMap<>();
    if (command.toUser() != null) {
      body.put("touser", command.toUser());
    }
    if (command.toParty() != null) {
      body.put("toparty", command.toParty());
    }
    if (command.toTag() != null) {
      body.put("totag", command.toTag());
    }
    body.put("msgtype", "text");
    body.put("agentid", Integer.parseInt(command.agentId()));
    body.put("text", Map.of("content", command.text()));
    body.put("safe", command.safe());
    return body;
  }

  private static void ensureSuccess(int statusCode, JsonNode root, String operation) {
    int errorCode = root.path("errcode").asInt(-1);
    if (statusCode < 200 || statusCode >= 300 || errorCode != 0) {
      throw new IllegalStateException(operation + " failed: " + readOptionalText(root, "errmsg"));
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
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
