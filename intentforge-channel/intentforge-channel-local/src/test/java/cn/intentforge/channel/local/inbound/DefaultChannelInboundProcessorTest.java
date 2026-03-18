package cn.intentforge.channel.local.inbound;

import cn.intentforge.channel.ChannelAccessDecision;
import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelCapability;
import cn.intentforge.channel.ChannelDescriptor;
import cn.intentforge.channel.ChannelDriver;
import cn.intentforge.channel.ChannelInboundDispatch;
import cn.intentforge.channel.ChannelInboundMessage;
import cn.intentforge.channel.ChannelInboundMessageProcessingResult;
import cn.intentforge.channel.ChannelInboundMessageProcessor;
import cn.intentforge.channel.ChannelInboundProcessingResult;
import cn.intentforge.channel.ChannelOutboundRequest;
import cn.intentforge.channel.ChannelParticipant;
import cn.intentforge.channel.ChannelRouteDecision;
import cn.intentforge.channel.ChannelInboundSource;
import cn.intentforge.channel.ChannelInboundSourceType;
import cn.intentforge.channel.ChannelSession;
import cn.intentforge.channel.ChannelTarget;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookHandler;
import cn.intentforge.channel.ChannelWebhookRequest;
import cn.intentforge.channel.ChannelWebhookResponse;
import cn.intentforge.channel.ChannelWebhookResult;
import cn.intentforge.channel.local.registry.InMemoryChannelManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultChannelInboundProcessorTest {
  @Test
  void shouldEvaluateAccessPolicyAndResolveRouteForAllowedMessages() {
    InMemoryChannelManager manager = new InMemoryChannelManager();
    manager.register(new StaticInboundChannelDriver());
    ChannelAccountProfile accountProfile =
        new ChannelAccountProfile("telegram-account", ChannelType.TELEGRAM, "Telegram Bot", Map.of("botToken", "demo"));
    DefaultChannelInboundProcessor processor = new DefaultChannelInboundProcessor(
        manager,
        List.of(message -> new ChannelAccessDecision(true, "allowed")),
        List.of(message -> Optional.of(new ChannelRouteDecision(
            "application-alpha",
            "session-alpha",
            "agent-alpha",
            Map.of("source", "test")))));

    ChannelInboundProcessingResult result = processor.process(accountProfile, new ChannelWebhookRequest(
        "POST",
        Map.of(),
        Map.of(),
        "ignored"));

    Assertions.assertEquals(1, result.dispatches().size());
    ChannelInboundDispatch dispatch = result.dispatches().getFirst();
    Assertions.assertTrue(dispatch.accessDecision().allowed());
    Assertions.assertEquals("application-alpha", dispatch.routeDecision().orElseThrow().spaceId());
    Assertions.assertEquals("session-alpha", dispatch.routeDecision().orElseThrow().sessionId());
    Assertions.assertEquals("agent-alpha", dispatch.routeDecision().orElseThrow().agentId());
  }

  @Test
  void shouldSkipRouteResolutionWhenAccessIsDenied() {
    InMemoryChannelManager manager = new InMemoryChannelManager();
    manager.register(new StaticInboundChannelDriver());
    ChannelAccountProfile accountProfile =
        new ChannelAccountProfile("telegram-account", ChannelType.TELEGRAM, "Telegram Bot", Map.of("botToken", "demo"));
    AtomicInteger routeCalls = new AtomicInteger();
    DefaultChannelInboundProcessor processor = new DefaultChannelInboundProcessor(
        manager,
        List.of(message -> new ChannelAccessDecision(false, "blocked")),
        List.of(message -> {
          routeCalls.incrementAndGet();
          return Optional.of(new ChannelRouteDecision("application-alpha", "session-alpha", null, Map.of()));
        }));

    ChannelInboundProcessingResult result = processor.process(accountProfile, new ChannelWebhookRequest(
        "POST",
        Map.of(),
        Map.of(),
        "ignored"));

    Assertions.assertEquals(1, result.dispatches().size());
    Assertions.assertFalse(result.dispatches().getFirst().accessDecision().allowed());
    Assertions.assertTrue(result.dispatches().getFirst().routeDecision().isEmpty());
    Assertions.assertEquals(0, routeCalls.get());
  }

  @Test
  void shouldProcessNormalizedMessagesThroughSharedMessageProcessor() {
    InMemoryChannelManager manager = new InMemoryChannelManager();
    manager.register(new StaticInboundChannelDriver());
    ChannelAccountProfile accountProfile =
        new ChannelAccountProfile("telegram-account", ChannelType.TELEGRAM, "Telegram Bot", Map.of("botToken", "demo"));
    ChannelInboundMessageProcessor processor = new DefaultChannelInboundProcessor(
        manager,
        List.of(message -> new ChannelAccessDecision(true, "allowed")),
        List.of(message -> Optional.of(new ChannelRouteDecision(
            "application-alpha",
            "session-alpha",
            null,
            Map.of("source", "test")))));

    ChannelInboundMessageProcessingResult result = processor.process(
        accountProfile,
        new ChannelInboundSource(ChannelInboundSourceType.LONG_POLLING, Map.of("batchId", "poll-1")),
        List.of(new ChannelInboundMessage(
            "message-1",
            accountProfile.id(),
            ChannelType.TELEGRAM,
            new ChannelTarget(accountProfile.id(), "chat-1", null, null, Map.of()),
            new ChannelParticipant("sender-1", "Sender", false, Map.of()),
            "hello inbound",
            Map.of())));

    Assertions.assertEquals(1, result.dispatches().size());
    ChannelInboundDispatch dispatch = result.dispatches().getFirst();
    Assertions.assertTrue(dispatch.accessDecision().allowed());
    Assertions.assertEquals("session-alpha", dispatch.routeDecision().orElseThrow().sessionId());
    Assertions.assertEquals("LONG_POLLING", dispatch.message().metadata().get("inboundSourceType"));
  }

  private static final class StaticInboundChannelDriver implements ChannelDriver {
    private final ChannelDescriptor descriptor = new ChannelDescriptor(
        "static-inbound",
        ChannelType.TELEGRAM,
        "Static Inbound",
        List.of(ChannelCapability.RECEIVE_MESSAGES),
        Map.of());

    @Override
    public ChannelDescriptor descriptor() {
      return descriptor;
    }

    @Override
    public ChannelSession openSession(ChannelAccountProfile accountProfile) {
      throw new UnsupportedOperationException("outbound session not used in this test");
    }

    @Override
    public Optional<ChannelWebhookHandler> openWebhookHandler(ChannelAccountProfile accountProfile) {
      return Optional.of(ChannelWebhookHandler.fixed(new ChannelWebhookResult(
          List.of(new ChannelInboundMessage(
              "message-1",
              accountProfile.id(),
              accountProfile.type(),
              new ChannelTarget(accountProfile.id(), "chat-1", null, null, Map.of()),
              new ChannelParticipant("sender-1", "Sender", false, Map.of()),
              "hello inbound",
              Map.of("messageCreatedAt", Instant.ofEpochSecond(1_700_000_000L)))),
          new ChannelWebhookResponse(200, "text/plain; charset=utf-8", "OK", Map.of()))));
    }
  }
}
