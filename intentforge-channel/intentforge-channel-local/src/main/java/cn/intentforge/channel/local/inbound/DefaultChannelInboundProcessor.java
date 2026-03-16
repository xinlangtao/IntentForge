package cn.intentforge.channel.local.inbound;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import cn.intentforge.channel.ChannelAccessDecision;
import cn.intentforge.channel.ChannelAccessPolicy;
import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelInboundDispatch;
import cn.intentforge.channel.ChannelInboundMessage;
import cn.intentforge.channel.ChannelInboundProcessingResult;
import cn.intentforge.channel.ChannelInboundProcessor;
import cn.intentforge.channel.ChannelRouteDecision;
import cn.intentforge.channel.ChannelRouteResolver;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookHandler;
import cn.intentforge.channel.ChannelWebhookRequest;
import cn.intentforge.channel.ChannelWebhookResult;
import cn.intentforge.channel.registry.ChannelManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Default local {@link ChannelInboundProcessor} that connects webhook parsing, access policy, and route resolution.
 *
 * @since 1.0.0
 */
public final class DefaultChannelInboundProcessor implements ChannelInboundProcessor {
  private final ChannelManager channelManager;
  private final List<ChannelAccessPolicy> accessPolicies;
  private final List<ChannelRouteResolver> routeResolvers;

  /**
   * Creates one processor with explicit channel manager, access policies, and route resolvers.
   *
   * @param channelManager channel manager used to open webhook handlers
   * @param accessPolicies ordered access policies
   * @param routeResolvers ordered route resolvers
   */
  public DefaultChannelInboundProcessor(
      ChannelManager channelManager,
      List<ChannelAccessPolicy> accessPolicies,
      List<ChannelRouteResolver> routeResolvers
  ) {
    this.channelManager = Objects.requireNonNull(channelManager, "channelManager must not be null");
    this.accessPolicies = accessPolicies == null || accessPolicies.isEmpty()
        ? List.of(new AllowAllChannelAccessPolicy())
        : List.copyOf(accessPolicies);
    this.routeResolvers = routeResolvers == null || routeResolvers.isEmpty()
        ? List.of(new DefaultChannelRouteResolver())
        : List.copyOf(routeResolvers);
  }

  /**
   * Creates one processor by loading classpath access policies and route resolvers and appending local defaults.
   *
   * @param channelManager channel manager used to open webhook handlers
   * @param classLoader class loader used to discover policies and resolvers
   * @return created inbound processor
   */
  public static DefaultChannelInboundProcessor createAndLoad(ChannelManager channelManager, ClassLoader classLoader) {
    ClassLoader effectiveClassLoader = classLoader == null
        ? DefaultChannelInboundProcessor.class.getClassLoader()
        : classLoader;
    List<ChannelAccessPolicy> loadedAccessPolicies = ServiceLoader.load(ChannelAccessPolicy.class, effectiveClassLoader)
        .stream()
        .map(ServiceLoader.Provider::get)
        .toList();
    List<ChannelRouteResolver> loadedRouteResolvers = new ArrayList<>(ServiceLoader.load(ChannelRouteResolver.class, effectiveClassLoader)
        .stream()
        .map(ServiceLoader.Provider::get)
        .toList());
    loadedRouteResolvers.add(new DefaultChannelRouteResolver());
    return new DefaultChannelInboundProcessor(channelManager, loadedAccessPolicies, loadedRouteResolvers);
  }

  @Override
  public ChannelInboundProcessingResult process(ChannelAccountProfile accountProfile, ChannelWebhookRequest request) {
    Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    Objects.requireNonNull(request, "request must not be null");
    ChannelWebhookHandler webhookHandler = channelManager.openWebhookHandler(accountProfile)
        .orElseThrow(() -> new IllegalStateException("no channel webhook handler available for account " + accountProfile.id()));
    ChannelWebhookResult webhookResult = webhookHandler.handle(request);
    List<ChannelInboundDispatch> dispatches = new ArrayList<>();
    for (ChannelInboundMessage message : webhookResult.messages()) {
      ChannelAccessDecision accessDecision = evaluateAccess(message);
      Optional<ChannelRouteDecision> routeDecision = accessDecision.allowed()
          ? resolveRoute(message)
          : Optional.empty();
      dispatches.add(new ChannelInboundDispatch(message, accessDecision, routeDecision));
    }
    return new ChannelInboundProcessingResult(webhookResult.response(), dispatches);
  }

  private ChannelAccessDecision evaluateAccess(ChannelInboundMessage message) {
    String reason = null;
    for (ChannelAccessPolicy accessPolicy : accessPolicies) {
      ChannelAccessDecision decision = Objects.requireNonNull(
          accessPolicy.evaluate(message),
          "access decision must not be null");
      if (!decision.allowed()) {
        return decision;
      }
      if (decision.reason() != null) {
        reason = decision.reason();
      }
    }
    return new ChannelAccessDecision(true, reason == null ? "allowed" : reason);
  }

  private Optional<ChannelRouteDecision> resolveRoute(ChannelInboundMessage message) {
    for (ChannelRouteResolver routeResolver : routeResolvers) {
      Optional<ChannelRouteDecision> decision = Objects.requireNonNull(
          routeResolver.resolve(message),
          "route decision must not be null");
      if (decision.isPresent()) {
        return decision;
      }
    }
    return Optional.empty();
  }

  private static final class AllowAllChannelAccessPolicy implements ChannelAccessPolicy {
    @Override
    public ChannelAccessDecision evaluate(ChannelInboundMessage message) {
      Objects.requireNonNull(message, "message must not be null");
      return new ChannelAccessDecision(true, "allowed");
    }
  }

  private static final class DefaultChannelRouteResolver implements ChannelRouteResolver {
    @Override
    public Optional<ChannelRouteDecision> resolve(ChannelInboundMessage message) {
      Objects.requireNonNull(message, "message must not be null");
      String spaceId = firstNonBlank(metadataText(message.metadata(), "spaceId"), message.accountId());
      String sessionId = firstNonBlank(
          metadataText(message.metadata(), "sessionId"),
          fallbackSessionId(message.type(), message.target().conversationId(), message.target().threadId()));
      String agentId = metadataText(message.metadata(), "agentId");
      Map<String, String> attributes = new LinkedHashMap<>();
      attributes.put("channelType", message.type().name());
      attributes.put("accountId", message.accountId());
      attributes.put("conversationId", message.target().conversationId());
      if (message.target().threadId() != null) {
        attributes.put("threadId", message.target().threadId());
      }
      return Optional.of(new ChannelRouteDecision(spaceId, sessionId, agentId, Map.copyOf(attributes)));
    }

    private static String fallbackSessionId(ChannelType channelType, String conversationId, String threadId) {
      String baseId = channelType.name().toLowerCase(java.util.Locale.ROOT) + ":" + conversationId;
      String normalizedThreadId = normalize(threadId);
      return normalizedThreadId == null ? baseId : baseId + ":" + normalizedThreadId;
    }

    private static String metadataText(Map<String, Object> metadata, String key) {
      if (metadata == null || metadata.isEmpty() || metadata.get(key) == null) {
        return null;
      }
      return normalize(String.valueOf(metadata.get(key)));
    }

    private static String firstNonBlank(String first, String second) {
      String normalizedFirst = normalize(first);
      if (normalizedFirst != null) {
        return normalizedFirst;
      }
      return normalize(second);
    }
  }
}
