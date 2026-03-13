package cn.intentforge.boot.server;

import cn.intentforge.agent.core.AgentRunEvent;
import cn.intentforge.agent.core.AgentRunSnapshot;
import cn.intentforge.api.agent.AgentApiException;
import cn.intentforge.api.agent.AgentRunCancelRequest;
import cn.intentforge.api.agent.AgentRunController;
import cn.intentforge.api.agent.AgentRunCreateRequest;
import cn.intentforge.api.agent.AgentRunEventResponse;
import cn.intentforge.api.agent.AgentRunFeedbackRequest;
import cn.intentforge.api.agent.AgentRunResponse;
import cn.intentforge.api.agent.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

final class AgentRunHttpExchangeHandler {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private final AgentRunController controller;
  private final AgentRunEventBroker eventBroker;

  AgentRunHttpExchangeHandler(AgentRunController controller, AgentRunEventBroker eventBroker) {
    this.controller = Objects.requireNonNull(controller, "controller must not be null");
    this.eventBroker = Objects.requireNonNull(eventBroker, "eventBroker must not be null");
  }

  void handleRunsRoot(HttpExchange exchange) throws IOException {
    try {
      if (!"POST".equals(exchange.getRequestMethod()) || !"/api/agent-runs".equals(exchange.getRequestURI().getPath())) {
        writeJson(exchange, 404, new ErrorResponse("AGENT_RUN_NOT_FOUND", "path not found"));
        return;
      }
      AgentRunResponse response = controller.createRun(
          readJson(exchange, AgentRunCreateRequest.class),
          eventBroker::publish);
      writeJson(exchange, 201, response);
    } catch (AgentApiException ex) {
      writeJson(exchange, ex.statusCode(), ex.error());
    } finally {
      exchange.close();
    }
  }

  void handleRunResource(HttpExchange exchange) throws IOException {
    try {
      String path = exchange.getRequestURI().getPath();
      String[] segments = path.split("/");
      if (segments.length < 5 || !"api".equals(segments[1]) || !"agent-runs".equals(segments[2])) {
        writeJson(exchange, 404, new ErrorResponse("AGENT_RUN_NOT_FOUND", "path not found"));
        return;
      }
      String runId = segments[3];
      String resource = segments[4];
      switch (resource) {
        case "events" -> handleEvents(exchange, runId);
        case "messages" -> handleMessages(exchange, runId);
        case "cancel" -> handleCancel(exchange, runId);
        default -> writeJson(exchange, 404, new ErrorResponse("AGENT_RUN_NOT_FOUND", "path not found"));
      }
    } finally {
      exchange.close();
    }
  }

  private void handleMessages(HttpExchange exchange, String runId) throws IOException {
    if (!"POST".equals(exchange.getRequestMethod())) {
      writeJson(exchange, 405, new ErrorResponse("METHOD_NOT_ALLOWED", "method not allowed"));
      return;
    }
    try {
      AgentRunResponse response = controller.resumeRun(
          runId,
          readJson(exchange, AgentRunFeedbackRequest.class),
          eventBroker::publish);
      writeJson(exchange, 200, response);
    } catch (AgentApiException ex) {
      writeJson(exchange, ex.statusCode(), ex.error());
    }
  }

  private void handleCancel(HttpExchange exchange, String runId) throws IOException {
    if (!"POST".equals(exchange.getRequestMethod())) {
      writeJson(exchange, 405, new ErrorResponse("METHOD_NOT_ALLOWED", "method not allowed"));
      return;
    }
    try {
      AgentRunResponse response = controller.cancelRun(
          runId,
          readJson(exchange, AgentRunCancelRequest.class),
          eventBroker::publish);
      writeJson(exchange, 200, response);
    } catch (AgentApiException ex) {
      writeJson(exchange, ex.statusCode(), ex.error());
    }
  }

  private void handleEvents(HttpExchange exchange, String runId) throws IOException {
    if (!"GET".equals(exchange.getRequestMethod())) {
      writeJson(exchange, 405, new ErrorResponse("METHOD_NOT_ALLOWED", "method not allowed"));
      return;
    }
    AgentRunSnapshot snapshot;
    try {
      snapshot = controller.getRunSnapshot(runId);
    } catch (AgentApiException ex) {
      writeJson(exchange, ex.statusCode(), ex.error());
      return;
    }

    long fromSequence = parseFromSequence(exchange.getRequestURI().getQuery(), exchange.getRequestHeaders());
    Headers headers = exchange.getResponseHeaders();
    headers.set("Content-Type", "text/event-stream; charset=utf-8");
    headers.set("Cache-Control", "no-cache");
    headers.set("Connection", "keep-alive");
    exchange.sendResponseHeaders(200, 0);

    try (OutputStream outputStream = exchange.getResponseBody();
         AgentRunEventBroker.Subscription subscription = eventBroker.subscribe(runId)) {
      for (AgentRunEvent event : snapshot.events()) {
        if (event.sequence() > fromSequence) {
          writeSseEvent(outputStream, controller.toEventResponse(event));
        }
      }
      if (snapshot.status().isTerminal()) {
        return;
      }
      BlockingQueue<AgentRunEvent> queue = subscription.queue();
      while (true) {
        AgentRunEvent event = queue.take();
        if (event.sequence() <= fromSequence) {
          continue;
        }
        writeSseEvent(outputStream, controller.toEventResponse(event));
        if (event.status().isTerminal()) {
          return;
        }
      }
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    } catch (IOException ignored) {
      // client disconnected
    }
  }

  private static long parseFromSequence(String query, Headers headers) {
    String headerValue = headers.getFirst("Last-Event-ID");
    long headerSequence = parseLong(headerValue);
    long querySequence = 0L;
    if (query != null && !query.isBlank()) {
      for (String pair : query.split("&")) {
        String[] parts = pair.split("=", 2);
        if (parts.length == 2 && "fromSequence".equals(parts[0])) {
          querySequence = parseLong(parts[1]);
          break;
        }
      }
    }
    return Math.max(headerSequence, querySequence);
  }

  private static long parseLong(String value) {
    if (value == null || value.isBlank()) {
      return 0L;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException ex) {
      return 0L;
    }
  }

  private static <T> T readJson(HttpExchange exchange, Class<T> type) throws IOException {
    return OBJECT_MAPPER.readValue(exchange.getRequestBody(), type);
  }

  private static void writeJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
    byte[] body = OBJECT_MAPPER.writeValueAsBytes(payload);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(statusCode, body.length);
    exchange.getResponseBody().write(body);
  }

  private static void writeSseEvent(OutputStream outputStream, AgentRunEventResponse event) throws IOException {
    writeLine(outputStream, "id: " + event.sequence());
    writeLine(outputStream, "event: " + event.type());
    writeLine(outputStream, "data: " + OBJECT_MAPPER.writeValueAsString(event));
    writeLine(outputStream, "");
    outputStream.flush();
  }

  private static void writeLine(OutputStream outputStream, String line) throws IOException {
    outputStream.write((line + "\n").getBytes(StandardCharsets.UTF_8));
  }
}
