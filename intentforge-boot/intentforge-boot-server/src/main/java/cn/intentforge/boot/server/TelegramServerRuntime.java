package cn.intentforge.boot.server;

import cn.intentforge.channel.telegram.inbound.TelegramInboundMode;

record TelegramServerRuntime(
    AiAssetServerRuntime serverRuntime,
    AutoCloseable telegramInboundRuntime,
    TelegramInboundMode inboundMode
) implements AutoCloseable {
  @Override
  public void close() throws Exception {
    try {
      telegramInboundRuntime.close();
    } finally {
      serverRuntime.close();
    }
  }
}
