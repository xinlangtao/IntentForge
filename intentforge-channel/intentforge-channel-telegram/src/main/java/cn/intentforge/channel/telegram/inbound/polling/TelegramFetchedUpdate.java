package cn.intentforge.channel.telegram.inbound.polling;

record TelegramFetchedUpdate(long updateId, String payload) {
}
