package cn.intentforge.config;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RuntimeImplementationDescriptorTest {
  @Test
  void shouldRequireNonBlankVersion() {
    IllegalArgumentException exception = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new RuntimeImplementationDescriptor(
            "prompt-a",
            RuntimeCapability.PROMPT_MANAGER,
            "Prompt A",
            "  ",
            "example.PromptA",
            Map.of()));

    Assertions.assertEquals("version must not be blank", exception.getMessage());
  }

  @Test
  void shouldTrimVersionText() {
    RuntimeImplementationDescriptor descriptor = new RuntimeImplementationDescriptor(
        "prompt-a",
        RuntimeCapability.PROMPT_MANAGER,
        "Prompt A",
        " nightly-SNAPSHOT ",
        "example.PromptA",
        Map.of("builtin", "true"));

    Assertions.assertEquals("nightly-SNAPSHOT", descriptor.version());
  }
}
