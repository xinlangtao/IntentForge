package cn.intentforge.channel.connectors.telegram;

interface TelegramBotApiClient {
  TelegramSendResult sendMessage(TelegramSendCommand command);
}
