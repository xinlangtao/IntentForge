package cn.intentforge.channel.telegram.inbound.polling;

import java.util.List;

interface TelegramLongPollingApiClient {
  List<TelegramFetchedUpdate> getUpdates(TelegramGetUpdatesCommand command);
}
