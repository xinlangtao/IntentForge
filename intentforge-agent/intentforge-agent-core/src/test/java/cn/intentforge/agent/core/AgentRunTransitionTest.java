package cn.intentforge.agent.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgentRunTransitionTest {
  @Test
  void shouldAcceptRoleBasedTransition() {
    AgentRunTransition transition = new AgentRunTransition("please code it", null, AgentRole.CODER, false);

    Assertions.assertEquals("please code it", transition.feedback());
    Assertions.assertEquals(AgentRole.CODER, transition.nextRole());
    Assertions.assertNull(transition.nextAgentId());
    Assertions.assertFalse(transition.complete());
  }

  @Test
  void shouldAcceptAgentBasedTransition() {
    AgentRunTransition transition = new AgentRunTransition("use alternate coder", "intentforge.native.coder.alt", null, false);

    Assertions.assertEquals("intentforge.native.coder.alt", transition.nextAgentId());
    Assertions.assertNull(transition.nextRole());
    Assertions.assertFalse(transition.complete());
  }

  @Test
  void shouldAcceptExplicitCompletionTransition() {
    AgentRunTransition transition = new AgentRunTransition("plan looks good", null, null, true);

    Assertions.assertTrue(transition.complete());
    Assertions.assertNull(transition.nextAgentId());
    Assertions.assertNull(transition.nextRole());
  }

  @Test
  void shouldRejectTransitionWithoutActionSelection() {
    IllegalArgumentException exception = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new AgentRunTransition("continue", null, null, false));

    Assertions.assertTrue(exception.getMessage().contains("nextRole"));
  }

  @Test
  void shouldRejectCompletionCombinedWithAgentSelection() {
    IllegalArgumentException exception = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new AgentRunTransition("finish", "intentforge.native.coder", null, true));

    Assertions.assertTrue(exception.getMessage().contains("complete"));
  }
}
