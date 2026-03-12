package cn.intentforge.config;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RuntimeCatalogTest {
  @Test
  void shouldGroupDescriptorsByCapability() {
    RuntimeCatalog catalog = RuntimeCatalog.of(List.of(
        new RuntimeImplementationDescriptor(
            "prompt-b",
            RuntimeCapability.PROMPT_MANAGER,
            "Prompt B",
            "example.PromptB",
            Map.of()),
        new RuntimeImplementationDescriptor(
            "tool-a",
            RuntimeCapability.TOOL_REGISTRY,
            "Tool A",
            "example.ToolA",
            Map.of()),
        new RuntimeImplementationDescriptor(
            "prompt-a",
            RuntimeCapability.PROMPT_MANAGER,
            "Prompt A",
            "example.PromptA",
            Map.of())));

    Assertions.assertEquals(
        List.of("prompt-a", "prompt-b"),
        catalog.list(RuntimeCapability.PROMPT_MANAGER).stream().map(RuntimeImplementationDescriptor::id).toList());
    Assertions.assertEquals("tool-a", catalog.find(RuntimeCapability.TOOL_REGISTRY, "tool-a").orElseThrow().id());
  }
}
