package cn.intentforge.boot.local;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelInboundDispatch;
import cn.intentforge.channel.ChannelInboundMessage;
import cn.intentforge.channel.ChannelInboundProcessingResult;
import cn.intentforge.channel.ChannelInboundProcessor;
import cn.intentforge.channel.ChannelRouteDecision;
import cn.intentforge.channel.ChannelWebhookRequest;
import cn.intentforge.session.model.Session;
import cn.intentforge.session.model.SessionDraft;
import cn.intentforge.session.model.SessionMessageDraft;
import cn.intentforge.session.model.SessionMessageRole;
import cn.intentforge.session.registry.SessionManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class PersistingChannelInboundProcessor implements ChannelInboundProcessor {
  private final ChannelInboundProcessor delegate;
  private final SessionManager sessionManager;

  PersistingChannelInboundProcessor(ChannelInboundProcessor delegate, SessionManager sessionManager) {
    this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager must not be null");
  }

  @Override
  public ChannelInboundProcessingResult process(ChannelAccountProfile accountProfile, ChannelWebhookRequest request) {
    ChannelInboundProcessingResult result = delegate.process(accountProfile, request);
    for (ChannelInboundDispatch dispatch : result.dispatches()) {
      persist(dispatch);
    }
    return result;
  }

  private void persist(ChannelInboundDispatch dispatch) {
    Objects.requireNonNull(dispatch, "dispatch must not be null");
    if (!dispatch.accessDecision().allowed() || dispatch.routeDecision().isEmpty()) {
      return;
    }
    ChannelInboundMessage message = dispatch.message();
    if (message.text() == null) {
      return;
    }
    ChannelRouteDecision routeDecision = dispatch.routeDecision().orElseThrow();
    Session session = sessionManager.find(routeDecision.sessionId())
        .orElseGet(() -> sessionManager.create(new SessionDraft(
            routeDecision.sessionId(),
            sessionTitle(message, routeDecision),
            routeDecision.spaceId(),
            sessionMetadata(message, routeDecision))));
    if (containsMessage(session, message.messageId())) {
      return;
    }
    sessionManager.appendMessage(routeDecision.sessionId(), new SessionMessageDraft(
        message.messageId(),
        SessionMessageRole.USER,
        message.text(),
        messageMetadata(message, routeDecision)));
  }

  private static boolean containsMessage(Session session, String messageId) {
    return session.messages().stream().anyMatch(message -> message.id().equals(messageId));
  }

  private static String sessionTitle(ChannelInboundMessage message, ChannelRouteDecision routeDecision) {
    String senderName = message.sender().displayName();
    if (senderName != null) {
      return senderName;
    }
    String conversationId = message.target().conversationId();
    return conversationId == null ? routeDecision.sessionId() : conversationId;
  }

  private static Map<String, String> sessionMetadata(ChannelInboundMessage message, ChannelRouteDecision routeDecision) {
    Map<String, String> metadata = new LinkedHashMap<>();
    metadata.put("channelType", message.type().name());
    metadata.put("channelAccountId", message.accountId());
    metadata.put("conversationId", message.target().conversationId());
    putIfPresent(metadata, "threadId", message.target().threadId());
    putIfPresent(metadata, "recipientId", message.target().recipientId());
    putIfPresent(metadata, "agentId", routeDecision.agentId());
    return Map.copyOf(metadata);
  }

  private static Map<String, String> messageMetadata(ChannelInboundMessage message, ChannelRouteDecision routeDecision) {
    Map<String, String> metadata = new LinkedHashMap<>();
    metadata.put("channelType", message.type().name());
    metadata.put("channelAccountId", message.accountId());
    metadata.put("channelMessageId", message.messageId());
    metadata.put("conversationId", message.target().conversationId());
    putIfPresent(metadata, "threadId", message.target().threadId());
    putIfPresent(metadata, "recipientId", message.target().recipientId());
    putIfPresent(metadata, "senderId", message.sender().id());
    putIfPresent(metadata, "senderName", message.sender().displayName());
    putIfPresent(metadata, "agentId", routeDecision.agentId());
    for (Map.Entry<String, Object> entry : message.metadata().entrySet()) {
      if (entry.getValue() == null) {
        continue;
      }
      String normalizedKey = normalize(entry.getKey());
      String normalizedValue = normalize(String.valueOf(entry.getValue()));
      if (normalizedKey != null && normalizedValue != null) {
        metadata.put("channel." + normalizedKey, normalizedValue);
      }
    }
    return Map.copyOf(metadata);
  }

  private static void putIfPresent(Map<String, String> metadata, String key, String value) {
    String normalizedValue = normalize(value);
    if (normalizedValue != null) {
      metadata.put(key, normalizedValue);
    }
  }
}
