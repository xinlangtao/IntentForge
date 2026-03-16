package cn.intentforge.channel.wecom;

interface WeComApplicationApiClient {
  WeComAccessTokenResult fetchAccessToken(WeComAccessTokenCommand command);

  WeComSendResult sendText(WeComSendCommand command);
}
