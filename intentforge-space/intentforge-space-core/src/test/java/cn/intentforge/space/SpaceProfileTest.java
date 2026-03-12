package cn.intentforge.space;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SpaceProfileTest {
  @Test
  void shouldNormalizeMultiValueBindings() {
    SpaceProfile profile = new SpaceProfile(
        List.of(" skill-a "),
        List.of(" agent-a "),
        List.of(" prompt-a "),
        List.of(" tool-a "),
        List.of(" model-a "),
        List.of(" provider-a "),
        List.of(" memory-a "),
        Map.of(" region ", " cn "));

    Assertions.assertEquals(List.of("skill-a"), profile.skillIds());
    Assertions.assertEquals(List.of("agent-a"), profile.agentIds());
    Assertions.assertEquals(List.of("prompt-a"), profile.promptIds());
    Assertions.assertEquals(List.of("tool-a"), profile.toolIds());
    Assertions.assertEquals(List.of("model-a"), profile.modelIds());
    Assertions.assertEquals(List.of("provider-a"), profile.modelProviderIds());
    Assertions.assertEquals(List.of("memory-a"), profile.memoryIds());
    Assertions.assertEquals(Map.of("region", "cn"), profile.config());
  }

  @Test
  void shouldRejectBlankResourceIdentifier() {
    IllegalArgumentException exception = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new SpaceProfile(
            List.of(" "),
            null,
            null,
            null,
            null,
            null,
            null,
            null));

    Assertions.assertTrue(exception.getMessage().contains("skillIds item"));
  }
}
