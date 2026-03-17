package cn.intentforge.boot.server;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookPropertyNames;
import java.util.LinkedHashMap;
import java.util.Map;

record TelegramWebhookServerSettings(
    String accountId,
    String displayName,
    String botToken,
    String baseUrl,
    String webhookUrl,
    String webhookBaseUrl,
    String webhookSecretToken,
    String webhookAllowedUpdates,
    Integer webhookMaxConnections,
    boolean webhookDropPendingUpdates,
    boolean webhookAutoManage
) {
  ChannelAccountProfile toAccountProfile() {
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("botToken", botToken);
    putIfPresent(properties, "baseUrl", baseUrl);
    putIfPresent(properties, ChannelWebhookPropertyNames.WEBHOOK_URL, webhookUrl);
    putIfPresent(properties, ChannelWebhookPropertyNames.WEBHOOK_BASE_URL, webhookBaseUrl);
    putIfPresent(properties, ChannelWebhookPropertyNames.WEBHOOK_SECRET_TOKEN, webhookSecretToken);
    putIfPresent(properties, ChannelWebhookPropertyNames.WEBHOOK_EVENT_TYPES, webhookAllowedUpdates);
    if (webhookMaxConnections != null) {
      properties.put(ChannelWebhookPropertyNames.WEBHOOK_MAX_CONNECTIONS, String.valueOf(webhookMaxConnections));
    }
    properties.put(
        ChannelWebhookPropertyNames.WEBHOOK_DROP_PENDING_UPDATES,
        String.valueOf(webhookDropPendingUpdates));
    properties.put(ChannelWebhookPropertyNames.WEBHOOK_AUTO_MANAGE, String.valueOf(webhookAutoManage));
    return new ChannelAccountProfile(accountId, ChannelType.TELEGRAM, displayName, Map.copyOf(properties));
  }

  private static void putIfPresent(Map<String, String> target, String key, String value) {
    if (value != null && !value.isBlank()) {
      target.put(key, value);
    }
  }
}
