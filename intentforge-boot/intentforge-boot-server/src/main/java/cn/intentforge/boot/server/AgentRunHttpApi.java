package cn.intentforge.boot.server;

import cn.intentforge.agent.core.AgentExecutionException;
import cn.intentforge.agent.core.AgentRunEvent;
import cn.intentforge.agent.core.AgentRunGateway;
import cn.intentforge.agent.core.AgentRunSnapshot;
import cn.intentforge.agent.core.AgentRunStatus;
import cn.intentforge.agent.core.AgentTask;
import cn.intentforge.agent.core.TaskMode;
import cn.intentforge.api.agent.AgentRunCancelRequest;
import cn.intentforge.api.agent.AgentRunCreateRequest;
import cn.intentforge.api.agent.AgentRunEventResponse;
import cn.intentforge.api.agent.AgentRunFeedbackRequest;
import cn.intentforge.api.agent.AgentRunResponse;
import cn.intentforge.api.agent.ErrorResponse;
import cn.intentforge.api.agent.RuntimeImplementationResponse;
import cn.intentforge.session.model.Session;
import cn.intentforge.session.model.SessionDraft;
import cn.intentforge.session.registry.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

final class AgentRunHttpApi {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private final AgentRunGateway agentRunGateway;
  private final SessionManager sessionManager;
  private final AgentRunEventBroker eventBroker;

  AgentRunHttpApi(AgentRunGateway agentRunGateway, SessionManager sessionManager, AgentRunEventBroker eventBroker) {
    this.agentRunGateway = Objects.requireNonNull(agentRunGateway, "agentRunGateway must not be null");
    this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager must not be null");
    this.eventBroker = Objects.requireNonNull(eventBroker, "eventBroker must not be null");
  }

  void handleRunsRoot(HttpExchange exchange) throws IOException {
    try {
      if (!"POST".equals(exchange.getRequestMethod()) || !"/api/agent-runs".equals(exchange.getRequestURI().getPath())) {
        writeJson(exchange, 404, new ErrorResponse("AGENT_RUN_NOT_FOUND", "path not found"));
        return;
      }
      AgentRunCreateRequest request = readJson(exchange, AgentRunCreateRequest.class);
      String sessionId = resolveSessionId(request);
      AgentTask task = new AgentTask(
          request.taskId(),
          sessionId,
          request.spaceId(),
          Path.of(request.workspaceRoot()),
          TaskMode.valueOf(request.mode()),
          request.intent(),
          request.targetAgentId(),
          request.metadata());
      AgentRunSnapshot snapshot = agentRunGateway.start(task, eventBroker::publish);
      writeJson(exchange, 201, toResponse(snapshot));
    } catch (IllegalArgumentException ex) {
      writeJson(exchange, 400, new ErrorResponse("AGENT_RUN_REQUEST_INVALID", messageOrFallback(ex, "invalid create request")));
    } catch (AgentExecutionException ex) {
      writeJson(exchange, 400, new ErrorResponse("AGENT_RUN_REQUEST_INVALID", messageOrFallback(ex, "agent run request failed")));
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
      AgentRunFeedbackRequest request = readJson(exchange, AgentRunFeedbackRequest.class);
      AgentRunSnapshot snapshot = agentRunGateway.resume(runId, request.content(), eventBroker::publish);
      writeJson(exchange, 200, toResponse(snapshot));
    } catch (AgentExecutionException ex) {
      writeJson(exchange, statusForRunMutation(ex), errorForMutation(ex, "AGENT_RUN_RESUME_FAILED"));
    } catch (IllegalArgumentException ex) {
      writeJson(exchange, 400, new ErrorResponse("AGENT_RUN_REQUEST_INVALID", messageOrFallback(ex, "invalid feedback request")));
    }
  }

  private void handleCancel(HttpExchange exchange, String runId) throws IOException {
    if (!"POST".equals(exchange.getRequestMethod())) {
      writeJson(exchange, 405, new ErrorResponse("METHOD_NOT_ALLOWED", "method not allowed"));
      return;
    }
    try {
      AgentRunCancelRequest request = readJson(exchange, AgentRunCancelRequest.class);
      AgentRunSnapshot snapshot = agentRunGateway.cancel(runId, request.reason(), eventBroker::publish);
      writeJson(exchange, 200, toResponse(snapshot));
    } catch (AgentExecutionException ex) {
      writeJson(exchange, statusForRunMutation(ex), errorForMutation(ex, "AGENT_RUN_CANCEL_FAILED"));
    } catch (IllegalArgumentException ex) {
      writeJson(exchange, 400, new ErrorResponse("AGENT_RUN_REQUEST_INVALID", messageOrFallback(ex, "invalid cancel request")));
    }
  }

  private void handleEvents(HttpExchange exchange, String runId) throws IOException {
    if (!"GET".equals(exchange.getRequestMethod())) {
      writeJson(exchange, 405, new ErrorResponse("METHOD_NOT_ALLOWED", "method not allowed"));
      return;
    }
    AgentRunSnapshot snapshot;
    try {
      snapshot = agentRunGateway.get(runId);
    } catch (AgentExecutionException ex) {
      writeJson(exchange, 404, new ErrorResponse("AGENT_RUN_NOT_FOUND", messageOrFallback(ex, "run not found")));
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
          writeSseEvent(outputStream, toEventResponse(event));
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
        writeSseEvent(outputStream, toEventResponse(event));
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

  private static ErrorResponse errorForMutation(AgentExecutionException ex, String fallbackCode) {
    String message = messageOrFallback(ex, "agent run mutation failed");
    if (message.contains("run not found")) {
      return new ErrorResponse("AGENT_RUN_NOT_FOUND", message);
    }
    if (message.contains("awaiting") || message.contains("terminal")) {
      return new ErrorResponse("AGENT_RUN_STATE_CONFLICT", message);
    }
    return new ErrorResponse(fallbackCode, message);
  }

  private static int statusForRunMutation(AgentExecutionException ex) {
    String message = messageOrFallback(ex, "agent run mutation failed");
    if (message.contains("run not found")) {
      return 404;
    }
    return 409;
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

  private static AgentRunResponse toResponse(AgentRunSnapshot snapshot) {
    return new AgentRunResponse(
        snapshot.runId(),
        snapshot.task().id(),
        snapshot.task().sessionId(),
        snapshot.status().name(),
        snapshot.nextStepIndex(),
        snapshot.awaitingReason(),
        "/api/agent-runs/" + snapshot.runId() + "/events",
        snapshot.contextPack().runtimeSelection().implementations().values().stream()
            .map(AgentRunHttpApi::toRuntimeResponse)
            .toList(),
        snapshot.events().stream().map(AgentRunHttpApi::toEventResponse).toList());
  }

  private static RuntimeImplementationResponse toRuntimeResponse(
      cn.intentforge.config.RuntimeImplementationDescriptor descriptor
  ) {
    return new RuntimeImplementationResponse(
        descriptor.id(),
        descriptor.capability().name(),
        descriptor.displayName(),
        descriptor.implementationClass(),
        descriptor.metadata());
  }

  private static AgentRunEventResponse toEventResponse(AgentRunEvent event) {
    return new AgentRunEventResponse(
        event.runId(),
        event.sequence(),
        event.type().name(),
        event.status().name(),
        event.message(),
        event.metadata(),
        event.createdAt().toString());
  }

  private static String messageOrFallback(Throwable throwable, String fallback) {
    return throwable.getMessage() == null || throwable.getMessage().isBlank() ? fallback : throwable.getMessage();
  }

  private String resolveSessionId(AgentRunCreateRequest request) {
    if (request.sessionId() != null) {
      return request.sessionId();
    }
    if (request.spaceId() == null) {
      throw new IllegalArgumentException("spaceId must not be blank when sessionId is absent");
    }
    Session session = sessionManager.create(new SessionDraft(
        generatedSessionId(),
        sessionTitleFor(request.intent()),
        request.spaceId(),
        Map.of(
            "source", "agent-run-api",
            "taskId", request.taskId())));
    return session.id();
  }

  private static String generatedSessionId() {
    return "session-" + UUID.randomUUID();
  }

  private static String sessionTitleFor(String intent) {
    String normalizedIntent = intent == null || intent.isBlank() ? "Agent Run" : intent.trim();
    return normalizedIntent.length() <= 80 ? normalizedIntent : normalizedIntent.substring(0, 80);
  }
}
