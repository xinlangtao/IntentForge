package cn.intentforge.hook;

import cn.intentforge.channel.ChannelType;
import java.util.Locale;
import java.util.Objects;

/**
 * Resolves canonical externally visible hook paths for channel accounts.
 *
 * @since 1.0.0
 */
public final class HookEndpointPaths {
  private HookEndpointPaths() {
  }

  /**
   * Returns the canonical externally visible hook path for one channel account.
   *
   * @param channelType target channel type
   * @param accountId target account identifier
   * @return canonical hook path
   */
  public static String canonicalPath(ChannelType channelType, String accountId) {
    Objects.requireNonNull(channelType, "channelType must not be null");
    String nonBlankAccountId = Objects.requireNonNull(accountId, "accountId must not be null").trim();
    if (nonBlankAccountId.isEmpty()) {
      throw new IllegalArgumentException("accountId must not be blank");
    }
    return switch (channelType) {
      case TELEGRAM -> "/open-api/hooks/telegram/accounts/" + nonBlankAccountId + "/webhook";
      case WECOM -> "/open-api/hooks/wecom/accounts/" + nonBlankAccountId + "/callback";
      default ->
          "/open-api/hooks/channels/" + channelType.name().toLowerCase(Locale.ROOT) + "/accounts/" + nonBlankAccountId + "/webhook";
    };
  }
}
