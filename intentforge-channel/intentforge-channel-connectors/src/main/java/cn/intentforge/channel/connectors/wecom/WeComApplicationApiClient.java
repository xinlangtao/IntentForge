package cn.intentforge.channel.connectors.wecom;

interface WeComApplicationApiClient {
  WeComAccessTokenResult fetchAccessToken(WeComAccessTokenCommand command);

  WeComSendResult sendText(WeComSendCommand command);
}
