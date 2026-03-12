package cn.intentforge.space;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ResolvedSpaceProfileTest {
  @Test
  void shouldRejectBlankResolvedResourceIdentifier() {
    IllegalArgumentException exception = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new ResolvedSpaceProfile(
            "application-alpha",
            SpaceType.APPLICATION,
            List.of("company-root", "application-alpha"),
            List.of("skill-a"),
            List.of(" "),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            Map.of()));

    Assertions.assertTrue(exception.getMessage().contains("agentIds item"));
  }
}
