package cn.intentforge.boot.local;

import cn.intentforge.config.ResolvedRuntimeSelection;
import cn.intentforge.config.RuntimeBindings;
import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.config.RuntimeCatalog;
import cn.intentforge.config.RuntimeImplementationDescriptor;
import cn.intentforge.governance.agent.ResolvedAgentRuntime;
import cn.intentforge.model.local.registry.InMemoryModelManager;
import cn.intentforge.model.provider.local.registry.InMemoryModelProviderRegistry;
import cn.intentforge.prompt.local.registry.InMemoryPromptManager;
import cn.intentforge.session.local.registry.InMemorySessionManager;
import cn.intentforge.space.ResolvedSpaceProfile;
import cn.intentforge.space.SpaceType;
import cn.intentforge.tool.core.gateway.ToolGateway;
import cn.intentforge.tool.core.model.ToolCallRequest;
import cn.intentforge.tool.core.model.ToolCallResult;
import cn.intentforge.tool.core.model.ToolDefinition;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LocalRuntimeComponentResolverTest {
  @Test
  void shouldResolveConfiguredRuntimeBindingsAndExposeSelection() {
    LocalRuntimeComponentResolver resolver = new LocalRuntimeComponentResolver(
        RuntimeCatalog.of(List.of(
            new RuntimeImplementationDescriptor(
                "prompt-default",
                RuntimeCapability.PROMPT_MANAGER,
                "Prompt Default",
                InMemoryPromptManager.class.getName(),
                Map.of("default", "true")),
            new RuntimeImplementationDescriptor(
                "prompt-db",
                RuntimeCapability.PROMPT_MANAGER,
                "Prompt DB",
                InMemoryPromptManager.class.getName(),
                Map.of()),
            new RuntimeImplementationDescriptor(
                "model-default",
                RuntimeCapability.MODEL_MANAGER,
                "Model Default",
                InMemoryModelManager.class.getName(),
                Map.of("default", "true")),
            new RuntimeImplementationDescriptor(
                "provider-default",
                RuntimeCapability.MODEL_PROVIDER_REGISTRY,
                "Provider Default",
                InMemoryModelProviderRegistry.class.getName(),
                Map.of("default", "true")),
            new RuntimeImplementationDescriptor(
                "tool-default",
                RuntimeCapability.TOOL_REGISTRY,
                "Tool Default",
                StubToolGateway.class.getName(),
                Map.of("default", "true")),
            new RuntimeImplementationDescriptor(
                "tool-mcp",
                RuntimeCapability.TOOL_REGISTRY,
                "Tool MCP",
                StubToolGateway.class.getName(),
                Map.of()),
            new RuntimeImplementationDescriptor(
                "session-default",
                RuntimeCapability.SESSION_MANAGER,
                "Session Default",
                InMemorySessionManager.class.getName(),
                Map.of("default", "true")))),
        new LocalRuntimeComponentRegistry(
            Map.of(),
            Map.of(
                "prompt-default", new InMemoryPromptManager(),
                "prompt-db", new InMemoryPromptManager()),
            Map.of("model-default", new InMemoryModelManager()),
            Map.of("provider-default", new InMemoryModelProviderRegistry()),
            Map.of(),
            Map.of(
                "tool-default", new StubToolGateway("tool-default"),
                "tool-mcp", new StubToolGateway("tool-mcp")),
            Map.of("session-default", new InMemorySessionManager())),
        new ResolvedRuntimeSelection(Map.of(
            RuntimeCapability.SESSION_MANAGER,
            new RuntimeImplementationDescriptor(
                "session-default",
                RuntimeCapability.SESSION_MANAGER,
                "Session Default",
                InMemorySessionManager.class.getName(),
                Map.of("default", "true")))));

    ResolvedAgentRuntime runtime = resolver.resolve(new ResolvedSpaceProfile(
        "application-alpha",
        SpaceType.APPLICATION,
        List.of("company-root", "project-alpha", "product-alpha", "application-alpha"),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        Map.of(),
        RuntimeBindings.of(Map.of(
            RuntimeCapability.PROMPT_MANAGER, "prompt-db",
            RuntimeCapability.TOOL_REGISTRY, "tool-mcp"))));

    Assertions.assertSame(runtime.promptManager(), resolver.runtimeComponents().promptManager("prompt-db"));
    Assertions.assertSame(runtime.toolGateway(), resolver.runtimeComponents().toolGateway("tool-mcp"));
    Assertions.assertEquals("prompt-db", runtime.runtimeSelection().get(RuntimeCapability.PROMPT_MANAGER).orElseThrow().id());
    Assertions.assertEquals("tool-mcp", runtime.runtimeSelection().get(RuntimeCapability.TOOL_REGISTRY).orElseThrow().id());
    Assertions.assertEquals("session-default", runtime.runtimeSelection().get(RuntimeCapability.SESSION_MANAGER).orElseThrow().id());
  }

  @Test
  void shouldRejectUnavailableConfiguredRuntimeImplementation() {
    LocalRuntimeComponentResolver resolver = new LocalRuntimeComponentResolver(
        RuntimeCatalog.of(List.of(
            new RuntimeImplementationDescriptor(
                "prompt-default",
                RuntimeCapability.PROMPT_MANAGER,
                "Prompt Default",
                InMemoryPromptManager.class.getName(),
                Map.of("default", "true")),
            new RuntimeImplementationDescriptor(
                "model-default",
                RuntimeCapability.MODEL_MANAGER,
                "Model Default",
                InMemoryModelManager.class.getName(),
                Map.of("default", "true")),
            new RuntimeImplementationDescriptor(
                "provider-default",
                RuntimeCapability.MODEL_PROVIDER_REGISTRY,
                "Provider Default",
                InMemoryModelProviderRegistry.class.getName(),
                Map.of("default", "true")),
            new RuntimeImplementationDescriptor(
                "tool-default",
                RuntimeCapability.TOOL_REGISTRY,
                "Tool Default",
                StubToolGateway.class.getName(),
                Map.of("default", "true")))),
        new LocalRuntimeComponentRegistry(
            Map.of(),
            Map.of("prompt-default", new InMemoryPromptManager()),
            Map.of("model-default", new InMemoryModelManager()),
            Map.of("provider-default", new InMemoryModelProviderRegistry()),
            Map.of(),
            Map.of("tool-default", new StubToolGateway("tool-default")),
            Map.of()),
        ResolvedRuntimeSelection.empty());

    IllegalArgumentException exception = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> resolver.resolve(new ResolvedSpaceProfile(
            "application-alpha",
            SpaceType.APPLICATION,
            List.of("application-alpha"),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            Map.of(),
            RuntimeBindings.of(Map.of(RuntimeCapability.PROMPT_MANAGER, "prompt-missing")))));
    Assertions.assertTrue(exception.getMessage().contains("prompt-missing"));
  }

  @Test
  void shouldRejectAmbiguousRuntimeSelectionWithoutSpaceBinding() {
    LocalRuntimeComponentResolver resolver = new LocalRuntimeComponentResolver(
        RuntimeCatalog.of(List.of(
            new RuntimeImplementationDescriptor(
                "prompt-a",
                RuntimeCapability.PROMPT_MANAGER,
                "Prompt A",
                InMemoryPromptManager.class.getName(),
                Map.of()),
            new RuntimeImplementationDescriptor(
                "prompt-b",
                RuntimeCapability.PROMPT_MANAGER,
                "Prompt B",
                InMemoryPromptManager.class.getName(),
                Map.of()),
            new RuntimeImplementationDescriptor(
                "model-default",
                RuntimeCapability.MODEL_MANAGER,
                "Model Default",
                InMemoryModelManager.class.getName(),
                Map.of("default", "true")),
            new RuntimeImplementationDescriptor(
                "provider-default",
                RuntimeCapability.MODEL_PROVIDER_REGISTRY,
                "Provider Default",
                InMemoryModelProviderRegistry.class.getName(),
                Map.of("default", "true")),
            new RuntimeImplementationDescriptor(
                "tool-default",
                RuntimeCapability.TOOL_REGISTRY,
                "Tool Default",
                StubToolGateway.class.getName(),
                Map.of("default", "true")))),
        new LocalRuntimeComponentRegistry(
            Map.of(),
            Map.of(
                "prompt-a", new InMemoryPromptManager(),
                "prompt-b", new InMemoryPromptManager()),
            Map.of("model-default", new InMemoryModelManager()),
            Map.of("provider-default", new InMemoryModelProviderRegistry()),
            Map.of(),
            Map.of("tool-default", new StubToolGateway("tool-default")),
            Map.of()),
        ResolvedRuntimeSelection.empty());

    IllegalStateException exception = Assertions.assertThrows(
        IllegalStateException.class,
        () -> resolver.resolve(new ResolvedSpaceProfile(
            "application-alpha",
            SpaceType.APPLICATION,
            List.of("application-alpha"),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            Map.of(),
            RuntimeBindings.empty())));
    Assertions.assertTrue(exception.getMessage().contains("PROMPT_MANAGER"));
  }

  private static final class StubToolGateway implements ToolGateway {
    private final String runtimeId;

    private StubToolGateway(String runtimeId) {
      this.runtimeId = runtimeId;
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
      return ToolCallResult.success(runtimeId);
    }

    @Override
    public List<ToolDefinition> listTools() {
      return List.of(new ToolDefinition("intentforge.fs.list", runtimeId, Map.of(), false));
    }
  }
}
