package cn.intentforge.channel.connectors.wecom;

import static cn.intentforge.common.util.ValidationSupport.normalizeOptional;
import static cn.intentforge.common.util.ValidationSupport.requireText;

record WeComSendCommand(
    String baseUrl,
    String accessToken,
    String agentId,
    String toUser,
    String toParty,
    String toTag,
    String text,
    int safe
) {
  WeComSendCommand {
    baseUrl = requireText(baseUrl, "baseUrl");
    accessToken = requireText(accessToken, "accessToken");
    agentId = requireText(agentId, "agentId");
    toUser = normalizeOptional(toUser);
    toParty = normalizeOptional(toParty);
    toTag = normalizeOptional(toTag);
    text = requireText(text, "text");
    if (toUser == null && toParty == null && toTag == null) {
      throw new IllegalArgumentException("at least one WeCom recipient must be provided");
    }
  }
}
