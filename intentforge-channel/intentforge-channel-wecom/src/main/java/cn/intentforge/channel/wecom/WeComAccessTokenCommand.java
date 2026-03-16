package cn.intentforge.channel.wecom;

import static cn.intentforge.common.util.ValidationSupport.requireText;

record WeComAccessTokenCommand(
    String baseUrl,
    String corpId,
    String corpSecret
) {
  WeComAccessTokenCommand {
    baseUrl = requireText(baseUrl, "baseUrl");
    corpId = requireText(corpId, "corpId");
    corpSecret = requireText(corpSecret, "corpSecret");
  }
}
