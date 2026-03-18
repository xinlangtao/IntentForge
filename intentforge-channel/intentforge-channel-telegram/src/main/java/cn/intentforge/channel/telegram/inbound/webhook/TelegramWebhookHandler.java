package cn.intentforge.channel.telegram.inbound.webhook;

import static cn.intentforge.common.util.ValidationSupport.normalize;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelWebhookHandler;
import cn.intentforge.channel.ChannelWebhookRequest;
import cn.intentforge.channel.ChannelWebhookResponse;
import cn.intentforge.channel.ChannelWebhookResult;
import cn.intentforge.channel.telegram.inbound.common.TelegramInboundUpdateNormalizer;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Normalizes Telegram webhook updates into shared channel inbound messages.
 *
 * @since 1.0.0
 */
public final class TelegramWebhookHandler implements ChannelWebhookHandler {
  private static final ChannelWebhookResponse OK_RESPONSE =
      new ChannelWebhookResponse(200, "text/plain; charset=utf-8", "OK", Map.of());
  private static final ChannelWebhookResponse UNAUTHORIZED_RESPONSE =
      new ChannelWebhookResponse(401, "text/plain; charset=utf-8", "unauthorized", Map.of());
  private static final String SECRET_TOKEN_HEADER = "X-Telegram-Bot-Api-Secret-Token";

  private final ChannelAccountProfile accountProfile;
  private final TelegramInboundUpdateNormalizer normalizer;

  /**
   * Creates one webhook handler with the default Telegram update normalizer.
   *
   * @param accountProfile Telegram account profile
   */
  public TelegramWebhookHandler(ChannelAccountProfile accountProfile) {
    this(accountProfile, new TelegramInboundUpdateNormalizer());
  }

  TelegramWebhookHandler(ChannelAccountProfile accountProfile, TelegramInboundUpdateNormalizer normalizer) {
    this.accountProfile = Objects.requireNonNull(accountProfile, "accountProfile must not be null");
    this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
  }

  @Override
  public ChannelWebhookResult handle(ChannelWebhookRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    if (!"POST".equals(request.method())) {
      return new ChannelWebhookResult(
          List.of(),
          new ChannelWebhookResponse(
              405,
              "text/plain; charset=utf-8",
              "method not allowed",
              Map.of("Allow", "POST")));
    }
    if (!secretTokenMatches(request)) {
      return new ChannelWebhookResult(List.of(), UNAUTHORIZED_RESPONSE);
    }
    return new ChannelWebhookResult(normalizer.normalize(accountProfile, requireText(request.body(), "body")), OK_RESPONSE);
  }

  private boolean secretTokenMatches(ChannelWebhookRequest request) {
    String configuredSecretToken = configuredSecretToken();
    if (configuredSecretToken == null) {
      return true;
    }
    return configuredSecretToken.equals(request.firstHeader(SECRET_TOKEN_HEADER));
  }

  private String configuredSecretToken() {
    Object configuredSecretToken = accountProfile.properties().get("webhookSecretToken");
    return configuredSecretToken == null ? null : normalize(String.valueOf(configuredSecretToken));
  }
}
