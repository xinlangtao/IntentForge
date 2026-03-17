package cn.intentforge.hook;

import static cn.intentforge.channel.ChannelWebhookPropertyNames.WEBHOOK_AUTO_MANAGE;
import static cn.intentforge.channel.ChannelWebhookPropertyNames.WEBHOOK_BASE_URL;
import static cn.intentforge.channel.ChannelWebhookPropertyNames.WEBHOOK_DESIRED_STATE;
import static cn.intentforge.channel.ChannelWebhookPropertyNames.WEBHOOK_DROP_PENDING_UPDATES;
import static cn.intentforge.channel.ChannelWebhookPropertyNames.WEBHOOK_EVENT_TYPES;
import static cn.intentforge.channel.ChannelWebhookPropertyNames.WEBHOOK_MAX_CONNECTIONS;
import static cn.intentforge.channel.ChannelWebhookPropertyNames.WEBHOOK_SECRET_TOKEN;
import static cn.intentforge.channel.ChannelWebhookPropertyNames.WEBHOOK_URL;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelWebhookAdministration;
import cn.intentforge.channel.ChannelWebhookDeletion;
import cn.intentforge.channel.ChannelWebhookRegistration;
import cn.intentforge.channel.ChannelWebhookStatus;
import cn.intentforge.channel.registry.ChannelManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Reconciles managed channel webhook registrations for manually registered hook accounts.
 *
 * @since 1.0.0
 */
public final class HookWebhookAutoManager {
  private final HookAccountRegistry accountRegistry;
  private final ChannelManager channelManager;

  /**
   * Creates one auto-manager backed by the provided hook account registry and channel manager.
   *
   * @param accountRegistry hook-visible account registry
   * @param channelManager channel runtime manager
   */
  public HookWebhookAutoManager(HookAccountRegistry accountRegistry, ChannelManager channelManager) {
    this.accountRegistry = Objects.requireNonNull(accountRegistry, "accountRegistry must not be null");
    this.channelManager = Objects.requireNonNull(channelManager, "channelManager must not be null");
  }

  /**
   * Reconciles all managed hook-visible accounts against their upstream webhook state.
   *
   * @param defaultBaseUri fallback base URI derived from the running server
   * @return immutable management results for accounts that were actually managed
   */
  public List<HookWebhookAutoManagementResult> reconcile(URI defaultBaseUri) {
    URI nonNullDefaultBaseUri = Objects.requireNonNull(defaultBaseUri, "defaultBaseUri must not be null");
    List<HookWebhookAutoManagementResult> results = new ArrayList<>();
    for (ChannelAccountProfile accountProfile : accountRegistry.list()) {
      if (!Boolean.parseBoolean(accountProfile.properties().getOrDefault(WEBHOOK_AUTO_MANAGE, "false"))) {
        continue;
      }
      ChannelWebhookAdministration administration = channelManager.openWebhookAdministration(accountProfile)
          .orElseThrow(() -> new IllegalStateException(
              "channel driver does not support webhook administration: " + accountProfile.type()));
      DesiredState desiredState = DesiredState.from(accountProfile.properties().get(WEBHOOK_DESIRED_STATE));
      if (desiredState == DesiredState.UNREGISTERED) {
        administration.deleteWebhook(new ChannelWebhookDeletion(dropPendingUpdates(accountProfile), metadata(accountProfile)));
        results.add(new HookWebhookAutoManagementResult(
            accountProfile.id(),
            accountProfile.type(),
            HookWebhookAutoManagementOperation.DELETE,
            administration.getWebhookInfo().orElse(null)));
        continue;
      }
      administration.setWebhook(new ChannelWebhookRegistration(
          webhookUrl(accountProfile, nonNullDefaultBaseUri),
          accountProfile.properties().get(WEBHOOK_SECRET_TOKEN),
          parseEventTypes(accountProfile),
          maxConnections(accountProfile),
          dropPendingUpdates(accountProfile),
          metadata(accountProfile)));
      results.add(new HookWebhookAutoManagementResult(
          accountProfile.id(),
          accountProfile.type(),
          HookWebhookAutoManagementOperation.REGISTER,
          administration.getWebhookInfo().orElse(null)));
    }
    return List.copyOf(results);
  }

  private static URI webhookUrl(ChannelAccountProfile accountProfile, URI defaultBaseUri) {
    String explicitUrl = accountProfile.properties().get(WEBHOOK_URL);
    if (explicitUrl != null && !explicitUrl.isBlank()) {
      return URI.create(explicitUrl.trim());
    }
    String baseUrl = accountProfile.properties().get(WEBHOOK_BASE_URL);
    URI resolvedBaseUri = baseUrl == null || baseUrl.isBlank()
        ? defaultBaseUri
        : URI.create(baseUrl.trim());
    return resolvedBaseUri.resolve(HookEndpointPaths.canonicalPath(accountProfile.type(), accountProfile.id()));
  }

  private static List<String> parseEventTypes(ChannelAccountProfile accountProfile) {
    String configuredEventTypes = accountProfile.properties().get(WEBHOOK_EVENT_TYPES);
    if (configuredEventTypes == null || configuredEventTypes.isBlank()) {
      return List.of();
    }
    return List.of(configuredEventTypes.split(","))
        .stream()
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .toList();
  }

  private static Integer maxConnections(ChannelAccountProfile accountProfile) {
    String configuredMaxConnections = accountProfile.properties().get(WEBHOOK_MAX_CONNECTIONS);
    if (configuredMaxConnections == null || configuredMaxConnections.isBlank()) {
      return null;
    }
    return Integer.valueOf(configuredMaxConnections.trim());
  }

  private static boolean dropPendingUpdates(ChannelAccountProfile accountProfile) {
    return Boolean.parseBoolean(accountProfile.properties().getOrDefault(WEBHOOK_DROP_PENDING_UPDATES, "false"));
  }

  private static java.util.Map<String, String> metadata(ChannelAccountProfile accountProfile) {
    return java.util.Map.of("accountId", accountProfile.id(), "channelType", accountProfile.type().name());
  }

  private enum DesiredState {
    REGISTERED,
    UNREGISTERED;

    private static DesiredState from(String rawValue) {
      if (rawValue == null || rawValue.isBlank()) {
        return REGISTERED;
      }
      return DesiredState.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
    }
  }
}
