package cn.intentforge.boot.server;

import cn.intentforge.api.agent.AgentRunCancelRequest;
import cn.intentforge.api.agent.AgentRunCreateRequest;
import cn.intentforge.api.agent.AgentRunEventResponse;
import cn.intentforge.api.agent.AgentRunFeedbackRequest;
import cn.intentforge.api.agent.AgentRunResponse;
import cn.intentforge.boot.local.AiAssetLocalRuntime;
import cn.intentforge.model.catalog.ModelCapability;
import cn.intentforge.model.catalog.ModelDescriptor;
import cn.intentforge.model.catalog.ModelType;
import cn.intentforge.model.provider.ModelProvider;
import cn.intentforge.model.provider.ModelProviderDescriptor;
import cn.intentforge.model.provider.ModelProviderType;
import cn.intentforge.prompt.model.PromptDefinition;
import cn.intentforge.prompt.model.PromptKind;
import cn.intentforge.session.model.SessionDraft;
import cn.intentforge.space.SpaceDefinition;
import cn.intentforge.space.SpaceProfile;
import cn.intentforge.space.SpaceRegistry;
import cn.intentforge.space.SpaceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AiAssetServerBootstrapIntegrationTest {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void shouldCreateObserveResumeAndCompleteRunOverHttpAndSse() throws Exception {
    Path workspace = Files.createTempDirectory("boot-server-agent-workspace");
    Files.writeString(workspace.resolve("README.md"), "agent");

    try (AiAssetServerRuntime runtime = startServer()) {
      HttpClient client = HttpClient.newHttpClient();
      AgentRunResponse pausedAfterPlanner = createRun(client, runtime.baseUri(), workspace);
      Assertions.assertFalse(pausedAfterPlanner.selectedRuntimes().isEmpty());
      Assertions.assertTrue(pausedAfterPlanner.selectedRuntimes().stream()
          .anyMatch(runtimeSelection -> "PROMPT_MANAGER".equals(runtimeSelection.capability())));

      HttpRequest sseRequest = HttpRequest.newBuilder(runtime.baseUri().resolve(pausedAfterPlanner.eventsPath()))
          .timeout(Duration.ofSeconds(10))
          .header("Accept", "text/event-stream")
          .GET()
          .build();
      HttpResponse<InputStream> sseResponse = client.send(sseRequest, HttpResponse.BodyHandlers.ofInputStream());
      Assertions.assertEquals(200, sseResponse.statusCode());

      try (SseRecorder recorder = new SseRecorder(sseResponse.body())) {
        Assertions.assertEquals("RUN_CREATED", recorder.awaitEvent("RUN_CREATED").type());
        Assertions.assertEquals(
            Map.of(
                "PROMPT_MANAGER", "intentforge.prompt.manager.in-memory",
                "MODEL_MANAGER", "intentforge.model.manager.in-memory",
                "MODEL_PROVIDER_REGISTRY", "intentforge.model-provider.registry.in-memory",
                "TOOL_REGISTRY", "intentforge.tool.registry.in-memory",
                "SESSION_MANAGER", "intentforge.session.manager.in-memory"),
            recorder.awaitEvent("CONTEXT_RESOLVED").metadata().get("selectedRuntimeIds"));
        Assertions.assertEquals("AWAITING_USER", recorder.awaitEvent("AWAITING_USER").type());

        HttpResponse<String> firstResumeResponse = postJson(
            client,
            runtime.baseUri().resolve("/api/agent-runs/" + pausedAfterPlanner.runId() + "/messages"),
            new AgentRunFeedbackRequest("Please add validation details"));
        Assertions.assertEquals(200, firstResumeResponse.statusCode());
        AgentRunResponse pausedAfterCoder = OBJECT_MAPPER.readValue(firstResumeResponse.body(), AgentRunResponse.class);
        Assertions.assertEquals("AWAITING_USER", pausedAfterCoder.status());
        Assertions.assertEquals(2, pausedAfterCoder.nextStepIndex());
        Assertions.assertEquals("USER_FEEDBACK_RECEIVED", recorder.awaitEvent("USER_FEEDBACK_RECEIVED").type());
        Assertions.assertEquals("AWAITING_USER", recorder.awaitEvent("AWAITING_USER").type());

        HttpResponse<String> secondResumeResponse = postJson(
            client,
            runtime.baseUri().resolve("/api/agent-runs/" + pausedAfterPlanner.runId() + "/messages"),
            new AgentRunFeedbackRequest("Please finish the final review"));
        Assertions.assertEquals(200, secondResumeResponse.statusCode());
        AgentRunResponse completed = OBJECT_MAPPER.readValue(secondResumeResponse.body(), AgentRunResponse.class);
        Assertions.assertEquals("COMPLETED", completed.status());
        Assertions.assertEquals("RUN_COMPLETED", recorder.awaitEvent("RUN_COMPLETED").type());
      }
    }
  }

  @Test
  void shouldAutoCreateSessionWhenRequestDoesNotProvideSessionId() throws Exception {
    Path workspace = Files.createTempDirectory("boot-server-auto-session-workspace");
    Files.writeString(workspace.resolve("README.md"), "agent");

    try (AiAssetServerRuntime runtime = startServer()) {
      HttpClient client = HttpClient.newHttpClient();
      HttpResponse<String> response = postJson(
          client,
          runtime.baseUri().resolve("/api/agent-runs"),
          new AgentRunCreateRequest(
              "task-auto-session",
              null,
              "application-alpha",
              workspace.toString(),
              "FULL",
              "Implement event-driven agent runtime",
              null,
              Map.of("story", "IF-503")));
      Assertions.assertEquals(201, response.statusCode());

      AgentRunResponse pausedAfterPlanner = OBJECT_MAPPER.readValue(response.body(), AgentRunResponse.class);
      Assertions.assertEquals("AWAITING_USER", pausedAfterPlanner.status());
      Assertions.assertNotNull(pausedAfterPlanner.sessionId());
      Assertions.assertFalse(pausedAfterPlanner.sessionId().isBlank());
      Assertions.assertNotEquals("session-1", pausedAfterPlanner.sessionId());
      Assertions.assertEquals(
          "application-alpha",
          runtime.localRuntime().sessionManager().find(pausedAfterPlanner.sessionId()).orElseThrow().spaceId());
    }
  }

  @Test
  void shouldRejectAutoSessionCreationWithoutSpaceId() throws Exception {
    Path workspace = Files.createTempDirectory("boot-server-missing-space-workspace");
    Files.writeString(workspace.resolve("README.md"), "agent");

    try (AiAssetServerRuntime runtime = startServer()) {
      HttpClient client = HttpClient.newHttpClient();
      HttpResponse<String> response = postJson(
          client,
          runtime.baseUri().resolve("/api/agent-runs"),
          new AgentRunCreateRequest(
              "task-missing-space",
              null,
              null,
              workspace.toString(),
              "FULL",
              "Implement event-driven agent runtime",
              null,
              Map.of("story", "IF-504")));
      Assertions.assertEquals(400, response.statusCode());
      cn.intentforge.api.agent.ErrorResponse error = OBJECT_MAPPER.readValue(
          response.body(),
          cn.intentforge.api.agent.ErrorResponse.class);
      Assertions.assertEquals("AGENT_RUN_REQUEST_INVALID", error.code());
      Assertions.assertTrue(error.message().contains("spaceId"));
    }
  }

  @Test
  void shouldCancelRunAndPreferVirtualThreadRequestExecutor() throws Exception {
    Path workspace = Files.createTempDirectory("boot-server-cancel-workspace");
    Files.writeString(workspace.resolve("README.md"), "agent");

    try (AiAssetServerRuntime runtime = startServer()) {
      Assertions.assertTrue(runtime.requestExecutor().submit(() -> Thread.currentThread().isVirtual()).get(5, TimeUnit.SECONDS));

      HttpClient client = HttpClient.newHttpClient();
      AgentRunResponse pausedAfterPlanner = createRun(client, runtime.baseUri(), workspace);

      HttpResponse<String> cancelResponse = postJson(
          client,
          runtime.baseUri().resolve("/api/agent-runs/" + pausedAfterPlanner.runId() + "/cancel"),
          new AgentRunCancelRequest("User stopped the run"));
      Assertions.assertEquals(200, cancelResponse.statusCode());
      AgentRunResponse cancelled = OBJECT_MAPPER.readValue(cancelResponse.body(), AgentRunResponse.class);
      Assertions.assertEquals("CANCELLED", cancelled.status());
      Assertions.assertTrue(cancelled.awaitingReason().contains("User stopped the run"));
    }
  }

  private static AgentRunResponse createRun(HttpClient client, URI baseUri, Path workspace) throws Exception {
    HttpResponse<String> response = postJson(
        client,
        baseUri.resolve("/api/agent-runs"),
        new AgentRunCreateRequest(
            "task-1",
            "session-1",
            null,
            workspace.toString(),
            "FULL",
            "Implement event-driven agent runtime",
            null,
            Map.of("story", "IF-501")));
    Assertions.assertEquals(201, response.statusCode());
    AgentRunResponse pausedAfterPlanner = OBJECT_MAPPER.readValue(response.body(), AgentRunResponse.class);
    Assertions.assertEquals("AWAITING_USER", pausedAfterPlanner.status());
    Assertions.assertEquals(1, pausedAfterPlanner.nextStepIndex());
    return pausedAfterPlanner;
  }

  private static HttpResponse<String> postJson(HttpClient client, URI uri, Object body) throws Exception {
    return client.send(
        HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private static AiAssetServerRuntime startServer() throws Exception {
    Path pluginsDirectory = Files.createTempDirectory("boot-server-plugins");
    return AiAssetServerBootstrap.bootstrap(
        new InetSocketAddress("127.0.0.1", 0),
        pluginsDirectory,
        AiAssetServerBootstrapIntegrationTest::registerSpaces,
        AiAssetServerBootstrapIntegrationTest::seedRuntime);
  }

  private static void registerSpaces(SpaceRegistry registry) {
    registry.registerAll(List.of(
        new SpaceDefinition("company-root", SpaceType.COMPANY, null, SpaceProfile.empty()),
        new SpaceDefinition("project-alpha", SpaceType.PROJECT, "company-root", SpaceProfile.empty()),
        new SpaceDefinition("product-alpha", SpaceType.PRODUCT, "project-alpha", SpaceProfile.empty()),
        new SpaceDefinition("application-alpha", SpaceType.APPLICATION, "product-alpha", new SpaceProfile(
            List.of(),
            List.of("intentforge.native.planner", "intentforge.native.coder", "intentforge.native.reviewer"),
            List.of("prompt-1"),
            List.of("intentforge.fs.list", "intentforge.runtime.environment.read"),
            List.of("model-1"),
            List.of("provider-1"),
            List.of(),
            Map.of("review.level", "mvp")))));
  }

  private static void seedRuntime(AiAssetLocalRuntime runtime) {
    runtime.promptManager().register(new PromptDefinition(
        "prompt-1",
        "v1",
        "Planner Prompt",
        "prompt",
        PromptKind.SYSTEM,
        List.of(),
        List.of("coding"),
        "Plan the task",
        Map.of()));
    runtime.modelManager().register(new ModelDescriptor(
        "model-1",
        "provider-1",
        "Coding Model",
        "model",
        ModelType.CHAT,
        List.of(ModelCapability.CHAT, ModelCapability.REASONING),
        32000,
        true,
        Map.of()));
    runtime.providerRegistry().register(new ModelProvider() {
      @Override
      public ModelProviderDescriptor descriptor() {
        return new ModelProviderDescriptor(
            "provider-1",
            "Provider",
            "provider",
            ModelProviderType.CUSTOM,
            "native://provider",
            List.of(ModelCapability.CHAT),
            Map.of());
      }
    });
    runtime.sessionManager().create(new SessionDraft("session-1", "Coding", "application-alpha", Map.of()));
  }

  private static final class SseRecorder implements AutoCloseable {
    private final InputStream inputStream;
    private final Thread task;
    private final BlockingQueue<AgentRunEventResponse> events = new LinkedBlockingQueue<>();

    private SseRecorder(InputStream inputStream) {
      this.inputStream = inputStream;
      this.task = Thread.ofVirtual().start(() -> {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
          String line;
          StringBuilder data = new StringBuilder();
          while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
              if (!data.isEmpty()) {
                events.offer(OBJECT_MAPPER.readValue(data.toString(), AgentRunEventResponse.class));
                data.setLength(0);
              }
              continue;
            }
            if (line.startsWith("data: ")) {
              data.append(line.substring("data: ".length()));
            }
          }
        } catch (Exception ignored) {
        }
      });
    }

    private AgentRunEventResponse awaitEvent(String type) throws Exception {
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
      while (System.nanoTime() < deadline) {
        AgentRunEventResponse event = events.poll(100, TimeUnit.MILLISECONDS);
        if (event != null && type.equals(event.type())) {
          return event;
        }
      }
      throw new AssertionError("event not received: " + type);
    }

    @Override
    public void close() throws Exception {
      inputStream.close();
      task.interrupt();
    }
  }
}
