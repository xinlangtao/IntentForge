package cn.intentforge.channel.telegram;

interface TelegramBotApiClient {
  TelegramSendResult sendMessage(TelegramSendCommand command);
}
