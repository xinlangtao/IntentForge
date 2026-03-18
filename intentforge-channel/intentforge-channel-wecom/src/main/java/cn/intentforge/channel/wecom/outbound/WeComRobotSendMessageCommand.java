package cn.intentforge.channel.wecom.outbound;

import static cn.intentforge.common.util.ValidationSupport.normalizeOptional;
import static cn.intentforge.common.util.ValidationSupport.requireText;

/**
 * Describes one outbound WeCom intelligent-robot text send command.
 *
 * @param baseUrl WeCom API base URL
 * @param robotId robot identifier
 * @param robotSecret robot secret
 * @param chatId optional chat identifier
 * @param userId optional user identifier
 * @param sessionId optional session identifier
 * @param text outbound text
 * @since 1.0.0
 */
public record WeComRobotSendMessageCommand(
    String baseUrl,
    String robotId,
    String robotSecret,
    String chatId,
    String userId,
    String sessionId,
    String text
) {
  /**
   * Creates one validated immutable command.
   */
  public WeComRobotSendMessageCommand {
    baseUrl = requireText(baseUrl, "baseUrl");
    robotId = requireText(robotId, "robotId");
    robotSecret = requireText(robotSecret, "robotSecret");
    chatId = normalizeOptional(chatId);
    userId = normalizeOptional(userId);
    sessionId = normalizeOptional(sessionId);
    text = requireText(text, "text");
    if (chatId == null && userId == null) {
      throw new IllegalArgumentException("chatId or userId must not both be blank");
    }
  }
}
