# intentforge-boot-server

Minimal server-side bootstrap entrypoint for the event-driven coding agent MVP.

Current implementation:
- JDK `HttpServer`
- HTTP JSON endpoints + SSE event stream
- request handling prefers virtual threads
- thin `HttpExchange` adapters that delegate business logic into `intentforge-api`
- create-run requests may auto-create one session when `sessionId` is omitted
- paused runs expose `availableNextActions`, and resume requests explicitly choose `nextRole`, `nextAgentId`, or `complete=true`
- terminal main: `cn.intentforge.boot.server.AiAssetServerMain`
- Telegram-focused terminal main: `cn.intentforge.boot.server.TelegramServerMain`
- Telegram webhook-only terminal main: `cn.intentforge.boot.server.TelegramWebhookServerMain`

Minimal startup:

```bash
cd /path/to/IntentForge

./mvnw -q -Drevision=nightly-SNAPSHOT \
  -pl intentforge-boot/intentforge-boot-server \
  -am \
  -DskipTests \
  package dependency:build-classpath \
  -Dmdep.outputFile=/tmp/intentforge-boot-server.cp

CLASSPATH="intentforge-boot/intentforge-boot-server/target/classes:$(cat /tmp/intentforge-boot-server.cp)"

java -Dintentforge.server.port=18080 \
  -cp "$CLASSPATH" \
  cn.intentforge.boot.server.AiAssetServerMain
```

Startup output includes:
- base URL like `http://127.0.0.1:18080`
- create run endpoint path
- request handling prefers virtual threads
- no runtime seed data is preloaded by the main entrypoint

For end-to-end demo data, use the integration fixtures under `src/test/java`.

## Telegram Local Inbound Smoke Test

`TelegramServerMain` starts the same HTTP server but adds one Telegram account whose inbound mode defaults to long polling.
The same entrypoint can switch to webhook mode through explicit settings, so one local startup flow can cover both Telegram inbound styles without a custom bootstrap class.
Webhook and long polling now share the same Telegram update normalizer and then converge into the same message-level inbound processor inside the local runtime.

### 1. Build the boot-server runtime classpath

```bash
cd /path/to/IntentForge

./mvnw -q -Drevision=nightly-SNAPSHOT \
  -pl intentforge-boot/intentforge-boot-server \
  -am \
  -DskipTests \
  package dependency:build-classpath \
  -Dmdep.outputFile=/tmp/intentforge-boot-server.cp

CLASSPATH="intentforge-boot/intentforge-boot-server/target/classes:$(cat /tmp/intentforge-boot-server.cp)"
```

### 2. Prepare Telegram settings

At minimum, set the bot token and one account identifier:

```bash
export TG_ACCOUNT_ID="telegram-demo"
export TG_DISPLAY_NAME="Telegram Demo Bot"
export TG_BOT_TOKEN="123456789:replace-with-real-token"
export TG_WEBHOOK_SECRET="telegram-local-secret"
```

Optional settings:

- `TG_INBOUND_MODE`: `LONG_POLLING` or `WEBHOOK`; defaults to `LONG_POLLING`
- `TG_WEBHOOK_BASE_URL`: public HTTPS base URL, for example from a reverse proxy or tunnel
- `TG_WEBHOOK_URL`: full public webhook URL override
- `TG_WEBHOOK_ALLOWED_UPDATES`: comma-separated Telegram update kinds such as `message,callback_query`
- `TG_WEBHOOK_MAX_CONNECTIONS`: optional `max_connections` override
- `TG_WEBHOOK_DROP_PENDING_UPDATES`: `true` or `false`
- `TG_WEBHOOK_AUTO_MANAGE`: when omitted, automatic webhook management turns on if `TG_WEBHOOK_BASE_URL` or `TG_WEBHOOK_URL` is present

You can verify the token before starting the server:

```bash
curl "https://api.telegram.org/bot${TG_BOT_TOKEN}/getMe"
```

### 3. Start the Telegram-focused server

If you do not set `TG_INBOUND_MODE`, the server starts in long-polling mode.
This mode does not need a public HTTP route because updates are pulled through Telegram Bot API `getUpdates`.

```bash
export TG_WEBHOOK_ALLOWED_UPDATES="message,callback_query"

java -Dintentforge.server.port=18080 \
  -cp "$CLASSPATH" \
  cn.intentforge.boot.server.TelegramServerMain
```

Startup prints:

- the local base URI
- the selected Telegram inbound mode
- the canonical Telegram webhook endpoint for `TG_ACCOUNT_ID`
- whether startup-time automatic webhook reconciliation is enabled for webhook mode

### 4. Verify the default long-polling mode

`TelegramServerMain` deletes an existing webhook before it starts long polling, so Telegram is not left in webhook mode accidentally.
You can inspect the upstream webhook state after the server starts:

```bash
curl "https://api.telegram.org/bot${TG_BOT_TOKEN}/getWebhookInfo"
```

The returned `url` should be empty or absent when long polling is active.
Then send a text message to the bot from the Telegram client while the process keeps running.

Current long-polling verification is intentionally minimal:

- there is no dedicated HTTP endpoint yet for reading back the persisted Telegram session
- the polled updates are consumed inside the in-process `SessionManager`

### 5. Switch to webhook mode

Webhook mode is opt-in.
For a real Telegram callback, point `TG_WEBHOOK_BASE_URL` or `TG_WEBHOOK_URL` at a public HTTPS address that forwards traffic to this process.
Telegram cannot deliver webhook traffic to `127.0.0.1` directly.

```bash
export TG_INBOUND_MODE="WEBHOOK"
export TG_WEBHOOK_BASE_URL="https://your-public-hook.example.com"
export TG_WEBHOOK_ALLOWED_UPDATES="message,callback_query"
export TG_WEBHOOK_AUTO_MANAGE="true"

java -Dintentforge.server.port=18080 \
  -cp "$CLASSPATH" \
  cn.intentforge.boot.server.TelegramServerMain
```

If you want a webhook-only compatibility entrypoint, this wrapper remains available:

```bash
java -Dintentforge.server.port=18080 \
  -cp "$CLASSPATH" \
  cn.intentforge.boot.server.TelegramWebhookServerMain
```

### 6. Verify webhook registration and the local hook route

When `TG_WEBHOOK_AUTO_MANAGE=true` and a public webhook URL can be derived, startup reconciles Telegram webhook state automatically.
You can inspect the registered webhook after the server starts:

```bash
curl "https://api.telegram.org/bot${TG_BOT_TOKEN}/getWebhookInfo"
```

The returned `url` should match:

```text
${TG_WEBHOOK_BASE_URL}/open-api/hooks/telegram/accounts/${TG_ACCOUNT_ID}/webhook
```

or the explicit `TG_WEBHOOK_URL` when you set one.

If you want to validate the hook path before involving Telegram, call the local endpoint directly.
This works even when no public tunnel is available.

```bash
curl -X POST "http://127.0.0.1:18080/open-api/hooks/telegram/accounts/${TG_ACCOUNT_ID}/webhook" \
  -H "Content-Type: application/json" \
  -H "X-Telegram-Bot-Api-Secret-Token: ${TG_WEBHOOK_SECRET}" \
  -d '{
    "update_id": 9001,
    "message": {
      "message_id": 1365,
      "date": 1710000000,
      "text": "hello from local curl",
      "chat": {
        "id": 123456789,
        "type": "private"
      },
      "from": {
        "id": 123456789,
        "is_bot": false,
        "first_name": "Local Tester"
      }
    }
  }'
```

Expected response:

```text
OK
```

### 7. Drive a real Telegram callback

Once the public HTTPS route is available and `getWebhookInfo` shows the expected URL:

1. Open the Telegram client and send a text message to the bot.
2. Keep the server process running so Telegram can POST to the registered webhook route.
3. Run `getWebhookInfo` again and verify `pending_update_count` returns to `0`.

Current inbound coverage is intentionally limited to:

- text-bearing `message`, `edited_message`, `channel_post`, and `edited_channel_post`
- `callback_query` payloads that carry `data` or `game_short_name`

### 8. Manual webhook control when auto-management is disabled

If you leave `TG_WEBHOOK_AUTO_MANAGE=false`, you can still register or remove the webhook yourself:

```bash
curl -X POST "https://api.telegram.org/bot${TG_BOT_TOKEN}/setWebhook" \
  -H "Content-Type: application/json" \
  -d "{\"url\":\"${TG_WEBHOOK_BASE_URL}/open-api/hooks/telegram/accounts/${TG_ACCOUNT_ID}/webhook\",\"secret_token\":\"${TG_WEBHOOK_SECRET}\",\"allowed_updates\":[\"message\",\"callback_query\"]}"

curl -X POST "https://api.telegram.org/bot${TG_BOT_TOKEN}/deleteWebhook?drop_pending_updates=true"
```
