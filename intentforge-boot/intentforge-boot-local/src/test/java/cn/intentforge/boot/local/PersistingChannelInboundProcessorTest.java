package cn.intentforge.boot.local;

import cn.intentforge.channel.ChannelAccessDecision;
import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelInboundDispatch;
import cn.intentforge.channel.ChannelInboundMessage;
import cn.intentforge.channel.ChannelInboundProcessingResult;
import cn.intentforge.channel.ChannelInboundProcessor;
import cn.intentforge.channel.ChannelParticipant;
import cn.intentforge.channel.ChannelRouteDecision;
import cn.intentforge.channel.ChannelTarget;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookRequest;
import cn.intentforge.channel.ChannelWebhookResponse;
import cn.intentforge.session.local.registry.InMemorySessionManager;
import cn.intentforge.session.model.Session;
import cn.intentforge.session.model.SessionDraft;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PersistingChannelInboundProcessorTest {
  @Test
  void shouldCreateSessionAndPersistInboundUserMessage() {
    InMemorySessionManager sessionManager = new InMemorySessionManager(
        getClass().getClassLoader(),
        Clock.fixed(Instant.ofEpochSecond(1_700_000_300L), ZoneOffset.UTC));
    PersistingChannelInboundProcessor processor = new PersistingChannelInboundProcessor(
        fixedDelegate(singleDispatch("message-1", "hello inbound", "application-alpha", "session-alpha")),
        sessionManager);

    ChannelInboundProcessingResult result = processor.process(accountProfile(), new ChannelWebhookRequest(
        "POST",
        Map.of(),
        Map.of(),
        "ignored"));

    Assertions.assertEquals(1, result.dispatches().size());
    Session session = sessionManager.find("session-alpha").orElseThrow();
    Assertions.assertEquals("application-alpha", session.spaceId());
    Assertions.assertEquals(1, session.messages().size());
    Assertions.assertEquals("message-1", session.messages().getFirst().id());
    Assertions.assertEquals("hello inbound", session.messages().getFirst().content());
    Assertions.assertEquals("USER", session.messages().getFirst().role().name());
  }

  @Test
  void shouldAppendInboundMessageToExistingSession() {
    InMemorySessionManager sessionManager = new InMemorySessionManager(
        getClass().getClassLoader(),
        Clock.fixed(Instant.ofEpochSecond(1_700_000_300L), ZoneOffset.UTC));
    sessionManager.create(new SessionDraft("session-alpha", "Existing Session", "application-alpha", Map.of()));
    PersistingChannelInboundProcessor processor = new PersistingChannelInboundProcessor(
        fixedDelegate(singleDispatch("message-2", "second inbound", "application-alpha", "session-alpha")),
        sessionManager);

    processor.process(accountProfile(), new ChannelWebhookRequest("POST", Map.of(), Map.of(), "ignored"));

    Session session = sessionManager.find("session-alpha").orElseThrow();
    Assertions.assertEquals(1, session.messages().size());
    Assertions.assertEquals("message-2", session.messages().getFirst().id());
  }

  @Test
  void shouldSkipDuplicateInboundMessagePersistence() {
    InMemorySessionManager sessionManager = new InMemorySessionManager(
        getClass().getClassLoader(),
        Clock.fixed(Instant.ofEpochSecond(1_700_000_300L), ZoneOffset.UTC));
    sessionManager.create(new SessionDraft("session-alpha", "Existing Session", "application-alpha", Map.of()));
    sessionManager.appendMessage("session-alpha", new cn.intentforge.session.model.SessionMessageDraft(
        "message-3",
        cn.intentforge.session.model.SessionMessageRole.USER,
        "already stored",
        Map.of("channelType", "TELEGRAM")));
    PersistingChannelInboundProcessor processor = new PersistingChannelInboundProcessor(
        fixedDelegate(singleDispatch("message-3", "already stored", "application-alpha", "session-alpha")),
        sessionManager);

    processor.process(accountProfile(), new ChannelWebhookRequest("POST", Map.of(), Map.of(), "ignored"));

    Session session = sessionManager.find("session-alpha").orElseThrow();
    Assertions.assertEquals(1, session.messages().size());
    Assertions.assertEquals("message-3", session.messages().getFirst().id());
  }

  private static ChannelAccountProfile accountProfile() {
    return new ChannelAccountProfile("telegram-account", ChannelType.TELEGRAM, "Telegram Bot", Map.of("botToken", "demo"));
  }

  private static ChannelInboundProcessor fixedDelegate(ChannelInboundDispatch dispatch) {
    return (accountProfile, request) -> new ChannelInboundProcessingResult(
        new ChannelWebhookResponse(200, "text/plain; charset=utf-8", "OK", Map.of()),
        List.of(dispatch));
  }

  private static ChannelInboundDispatch singleDispatch(
      String messageId,
      String text,
      String spaceId,
      String sessionId
  ) {
    return new ChannelInboundDispatch(
        new ChannelInboundMessage(
            messageId,
            "telegram-account",
            ChannelType.TELEGRAM,
            new ChannelTarget("telegram-account", "chat-1", null, null, Map.of()),
            new ChannelParticipant("sender-1", "Sender", false, Map.of()),
            text,
            Map.of("channelType", "TELEGRAM")),
        new ChannelAccessDecision(true, "allowed"),
        Optional.of(new ChannelRouteDecision(spaceId, sessionId, null, Map.of("source", "test"))));
  }
}
