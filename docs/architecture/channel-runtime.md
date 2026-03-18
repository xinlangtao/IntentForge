# IntentForge Channel Runtime

## Purpose

`intentforge-channel` provides the runtime spine for inbound and outbound messaging channels.
It is designed for pluggable multi-channel integrations such as Telegram and WeCom without pushing vendor-specific logic into the shared core model.

## Module Layout

| Module | Responsibility |
| --- | --- |
| `intentforge-channel-core` | Shared channel descriptors, account/target/message/webhook models, message-level and webhook-level inbound processing contracts, routing contracts, and manager/plugin SPI |
| `intentforge-channel-local` | In-memory `ChannelManager`, webhook-level and message-level inbound processing pipeline, classpath plugin loading, and external plugin directory loading |
| `intentforge-channel-spring` | Spring `spring.factories` discovery bridge that contributes `ChannelPlugin` instances through a dedicated SPI strategy |
| `intentforge-channel-connectors` | Loopback and generic connector support entrypoints |
| `intentforge-channel-telegram` | Telegram Bot API outbound connector, inbound long polling, inbound webhook normalization, and webhook administration |
| `intentforge-channel-wecom` | WeCom application messaging outbound connector and inbound callback normalization |

## Core Model

The shared runtime model is intentionally transport-neutral:

- `ChannelDescriptor`: describes one driver and its normalized capabilities
- `ChannelAccountProfile`: describes one configured account instance
- `ChannelTarget`: represents a normalized conversation/thread/recipient target
- `ChannelInboundMessage`: normalized inbound event payload
- `ChannelOutboundRequest`: normalized outbound send request
- `ChannelDeliveryResult`: normalized send result
- `ChannelWebhookRequest`: normalized inbound webhook HTTP envelope
- `ChannelWebhookResponse`: normalized webhook acknowledgement response
- `ChannelWebhookResult`: normalized webhook parsing output
- `ChannelWebhookHandler`: account-bound inbound webhook adapter
- `ChannelInboundSource`: describes whether normalized messages came from webhook or long polling
- `ChannelInboundMessageProcessor`: runtime entrypoint that processes already normalized inbound messages
- `ChannelInboundMessageProcessingResult`: aggregated message-level processing result
- `ChannelInboundDispatch`: per-message access and route evaluation result
- `ChannelInboundProcessingResult`: aggregated inbound pipeline result
- `ChannelInboundProcessor`: runtime entrypoint that chains webhook parsing, access policy, and route resolution
- `ChannelRouteResolver`: resolves inbound messages into `space/session/agent`
- `ChannelAccessPolicy`: evaluates whether one inbound event may enter the runtime

This keeps Telegram chat IDs, WeCom user IDs, thread IDs, and future platform-specific identifiers inside connector metadata instead of leaking them into the shared API surface.

## Extension Model

There are three extension layers:

1. `ChannelManagerProvider`
   - Discovered through JDK `ServiceLoader`
   - Exposes runtime implementations to `RuntimeCatalog`
   - Current builtin implementation: `InMemoryChannelManagerProvider`

2. `ChannelPlugin`
   - Contributes one or more `ChannelDriver` implementations
   - Loaded from classpath SPI and from external plugin jars

3. `ChannelPluginDiscoveryStrategy`
   - Extends plugin discovery with integration-specific mechanisms
   - Current Spring bridge implementation: `SpringFactoriesChannelPluginDiscoveryStrategy`

## Local Runtime Behavior

`InMemoryChannelManager` merges channel drivers from:

- direct classpath `ChannelPlugin` providers
- additional `ChannelPluginDiscoveryStrategy` providers

`DefaultChannelInboundProcessor` in `intentforge-channel-local` now exposes two layers:

- webhook-level processing through `ChannelInboundProcessor`
- message-level processing through `ChannelInboundMessageProcessor`

The webhook-level path chains:

- `ChannelManager.openWebhookHandler(accountProfile)`
- connector-specific webhook parsing into `ChannelInboundMessage`
- delegation into the shared message-level processor

The shared message-level path chains:

- ordered `ChannelAccessPolicy` implementations loaded from classpath `ServiceLoader`
- ordered `ChannelRouteResolver` implementations loaded from classpath `ServiceLoader`

When messages enter through the message-level processor, the local runtime injects `metadata.inboundSourceType` and optional `metadata.inboundSource.*` attributes before access and route evaluation.

Fallback behavior:

- when no access policy is discovered, the processor uses an allow-all policy
- when no route resolver matches, the local default route resolver maps:
  - `spaceId` -> `ChannelInboundMessage.accountId()`
  - `sessionId` -> `<channelType>:<conversationId>[:<threadId>]`
  - `agentId` -> optional `metadata.agentId`

`DirectoryChannelPluginManager` mirrors the existing plugin runtime pattern and loads external channel plugin jars from `plugins/`.

`intentforge-channel-local` intentionally stops at webhook parsing, access evaluation, and route resolution.
Session persistence remains outside the channel runtime module so the channel layer stays storage-agnostic.

## Spring SPI Bridge

Spring support is intentionally isolated in `intentforge-channel-spring`.

- Core and local modules do not depend on Spring APIs
- Spring-specific discovery is exposed through `ChannelPluginDiscoveryStrategy`
- `SpringFactoriesChannelPluginDiscoveryStrategy` loads `ChannelPlugin` implementations from `META-INF/spring.factories`

This keeps local bootstrap usable in non-Spring environments while still allowing Spring starter style channel packaging.

## Bootstrap Integration

`AiAssetLocalBootstrap` now discovers `ChannelManagerProvider` implementations, initializes the default `ChannelManager`, loads classpath plugins, and synchronizes external channel plugin jars.

The selected channel manager is exposed through:

- `RuntimeCapability.CHANNEL_MANAGER`
- `LocalRuntimeComponentRegistry`
- `AiAssetLocalRuntime`

The local runtime now exposes:

- one `ChannelInboundProcessor` for raw webhook requests
- one `ChannelInboundMessageProcessor` for already normalized inbound message batches

The webhook-level runtime path executes:

- webhook normalization inside the selected connector module
- delegation into the shared message-level processor
- session persistence into `SessionManager` for allowed and routed inbound text messages

The shared message-level runtime path executes:

- access-policy evaluation
- route resolution with fallback behavior
- session persistence into `SessionManager` for allowed and routed inbound text messages

Session persistence behavior:

- the local runtime creates the routed session when `sessionId` does not exist yet
- persisted inbound messages are stored as `SessionMessageRole.USER`
- duplicate inbound message ids are skipped to reduce retry-driven duplication
- session persistence is added by `PersistingChannelInboundProcessor` in `intentforge-boot-local`, not by `intentforge-channel-local`

The channel manager is currently bootstrap-scoped, similar to the session manager.

## Hook HTTP Ingress

`intentforge-hook` owns the externally exposed hook endpoint family that feeds channel inbound processing.
This keeps connector modules transport-agnostic while centralizing request-path parsing and account resolution in one module.

Current generic route:

- `/open-api/hooks/channels/{channelType}/accounts/{accountId}/webhook`

Current platform-specific routes:

- Telegram: `/open-api/hooks/telegram/accounts/{accountId}/webhook`
- WeCom: `/open-api/hooks/wecom/accounts/{accountId}/callback`

Ingress behavior:

- `HookHttpRouteRegistrar` registers the generic and platform-specific hook prefixes on the JDK `HttpServer`
- `ChannelWebhookHttpExchangeHandler`, `TelegramWebhookHttpExchangeHandler`, and `WeComWebhookHttpExchangeHandler` convert `HttpExchange` into `ChannelWebhookRequest`
- `ChannelWebhookEndpointController` resolves the target `ChannelAccountProfile` through `HookAccountRegistry`
- the resolved account and normalized request are delegated into `ChannelInboundProcessor`
- `AiAssetServerBootstrap` seeds hook-visible accounts through the `hookConfigurer` callback before the server starts
- `HookWebhookAutoManager` inspects manually registered hook-visible accounts and reconciles managed webhook lifecycles before the server starts accepting traffic
- `TelegramWebhookServerMain` in `intentforge-boot-server` is a convenience terminal entrypoint for local Telegram testing; it resolves one manual Telegram account from system properties or environment variables and delegates into the same bootstrap path
- unknown hook paths and unregistered hook accounts return `404`
- unexpected ingress failures return `500`

## Future Connector Guidance

Telegram and WeCom adapters should follow these rules:

- keep vendor DTOs and API clients inside connector modules
- map vendor events into `ChannelInboundMessage`
- map outbound send requests from `ChannelOutboundRequest`
- use `ChannelAccountProfile.properties` and request metadata for connector-specific settings
- keep routing and access control in shared contracts instead of hard-coded connector logic

## Builtin Connector Coverage

Concrete vendor connectors now live in dedicated child modules, while `intentforge-channel-connectors` keeps the loopback example.

### Telegram

- module: `intentforge-channel-telegram`
- plugin id: `intentforge.channel.telegram`
- runtime type: `ChannelType.TELEGRAM`
- package layout:
  - `plugin`: SPI entrypoint
  - `driver`: pluggable driver and account-bound runtime wiring
  - `outbound`: Bot API send client, command/result models, and outbound session
  - `inbound.common`: shared Telegram update normalization
  - `inbound.webhook`: webhook normalization
  - `inbound.polling`: long-polling client and background ingress
  - `admin`: webhook lifecycle administration
- current scope:
  - outbound text delivery via Telegram Bot API `sendMessage`
  - inbound long polling via Telegram Bot API `getUpdates`
  - inbound webhook normalization for text-bearing updates
  - outbound webhook lifecycle administration via Telegram Bot API `setWebhook`, `deleteWebhook`, and `getWebhookInfo`
- required account properties:
  - `botToken`: Telegram bot token
  - `baseUrl`: optional Bot API base URL, defaults to `https://api.telegram.org`
  - `inboundMode`: optional inbound mode, `LONG_POLLING` or `WEBHOOK`, defaults to `LONG_POLLING` in `TelegramServerMain`
  - `pollingAllowedUpdates`: optional comma-separated Telegram update kinds used when long polling calls `getUpdates`
  - `pollingDeleteWebhookOnStart`: optional boolean that controls whether long polling deletes an existing webhook before polling begins, defaults to `true`
  - `webhookSecretToken`: optional webhook secret token that must match `X-Telegram-Bot-Api-Secret-Token` on inbound requests
  - `webhookAutoManage`: optional boolean that enables startup-time automatic webhook reconciliation for the account
  - `webhookUrl`: optional full externally reachable webhook URL override used by automatic webhook reconciliation
  - `webhookBaseUrl`: optional externally reachable base URL used to compose the canonical Telegram hook path
  - `webhookDesiredState`: optional desired managed lifecycle state, `REGISTERED` or `UNREGISTERED`, defaults to `REGISTERED`
  - `webhookAllowedUpdates`: optional comma-separated Telegram update kinds used when automatic webhook reconciliation registers the webhook
  - `webhookMaxConnections`: optional `max_connections` override used during automatic webhook reconciliation
  - `webhookDropPendingUpdates`: optional boolean applied to both managed `setWebhook` and managed `deleteWebhook`
- target mapping:
  - `ChannelTarget.conversationId` -> `chat_id`
  - `ChannelTarget.threadId` -> `message_thread_id`
- outbound metadata mapping:
  - `parseMode` -> `parse_mode`
  - `disableNotification` -> `disable_notification`
  - `disableWebPagePreview` -> `link_preview_options.is_disabled`
- inbound webhook behavior:
  - `TelegramServerMain` defaults to `LONG_POLLING` and starts one background long-polling ingress for the configured Telegram account
  - both webhook and long polling reuse `TelegramInboundUpdateNormalizer` so Telegram update parsing lives in one place
  - long polling uses `getUpdates`, normalizes each raw update into `ChannelInboundMessage`, forwards the normalized batch into the shared `ChannelInboundMessageProcessor`, and deletes an existing webhook on startup unless `pollingDeleteWebhookOnStart=false`
  - webhook mode is explicit through `inboundMode=WEBHOOK` or `TG_INBOUND_MODE=WEBHOOK`
  - `POST` JSON updates are validated through `ChannelWebhookHandler`, normalized through the shared Telegram normalizer, and then delegated into the shared message-level processor
  - when `webhookSecretToken` is configured, inbound requests must provide the matching `X-Telegram-Bot-Api-Secret-Token` header
  - hook ingress supports both the generic route and `/open-api/hooks/telegram/accounts/{accountId}/webhook`
  - when `webhookAutoManage=true`, the server bootstrap opens an account-bound webhook administration facade, derives the public webhook URL from `webhookUrl`, `webhookBaseUrl`, or the running server base URI, and then reconciles Telegram webhook state before startup completes
  - managed `webhookDesiredState=REGISTERED` triggers `setWebhook` followed by `getWebhookInfo`
  - managed `webhookDesiredState=UNREGISTERED` triggers `deleteWebhook` followed by `getWebhookInfo`
  - `TelegramServerMain` accepts the same Telegram account settings via system properties or environment variables and keeps `TelegramWebhookServerMain` as a webhook-only compatibility wrapper
  - `message`, `edited_message`, `channel_post`, and `edited_channel_post` with `text` are normalized into one `ChannelInboundMessage`
  - `callback_query` updates with `data` or `game_short_name` are normalized into one `ChannelInboundMessage`, with the callback payload mapped into `text` and callback identifiers stored in metadata
  - non-text updates currently acknowledge with `200 OK` and produce no normalized messages

Telegram-focused local startup settings:

- system properties:
  - `intentforge.telegram.accountId`
  - `intentforge.telegram.displayName`
  - `intentforge.telegram.botToken`
  - `intentforge.telegram.baseUrl`
  - `intentforge.telegram.inboundMode`
  - `intentforge.telegram.webhookUrl`
  - `intentforge.telegram.webhookBaseUrl`
  - `intentforge.telegram.webhookSecretToken`
  - `intentforge.telegram.webhookAllowedUpdates`
  - `intentforge.telegram.webhookMaxConnections`
  - `intentforge.telegram.webhookDropPendingUpdates`
  - `intentforge.telegram.webhookAutoManage`
- environment variables:
  - `TG_ACCOUNT_ID`
  - `TG_DISPLAY_NAME`
  - `TG_BOT_TOKEN`
  - `TG_BASE_URL`
  - `TG_INBOUND_MODE`
  - `TG_WEBHOOK_URL`
  - `TG_WEBHOOK_BASE_URL`
  - `TG_WEBHOOK_SECRET`
  - `TG_WEBHOOK_ALLOWED_UPDATES`
  - `TG_WEBHOOK_MAX_CONNECTIONS`
  - `TG_WEBHOOK_DROP_PENDING_UPDATES`
  - `TG_WEBHOOK_AUTO_MANAGE`

For copy-paste startup commands plus a manual Telegram webhook smoke-test flow, see `intentforge-boot/intentforge-boot-server/README.md`.

### WeCom

- module: `intentforge-channel-wecom`
- plugin id: `intentforge.channel.wecom`
- runtime type: `ChannelType.WECOM`
- current scope:
  - outbound text delivery for WeCom application messaging
  - inbound callback normalization for verification handshakes and plaintext text callbacks
- required account properties:
  - `corpId`: enterprise identifier
  - `agentId`: application agent identifier
  - `corpSecret`: application secret
  - `baseUrl`: optional API base URL, defaults to `https://qyapi.weixin.qq.com`
- token flow:
  - fetch access token through `gettoken`
  - cache the token inside the opened channel session until close to expiry
- target mapping:
  - prefer `ChannelTarget.recipientId` as `touser`
  - otherwise use `ChannelTarget.conversationId` as `touser`
- outbound metadata mapping:
  - `toParty` -> `toparty`
  - `toTag` -> `totag`
  - `safe` -> `safe`
- inbound webhook behavior:
  - `GET` verification requests echo `echostr` without signature validation
  - hook ingress supports both the generic route and `/open-api/hooks/wecom/accounts/{accountId}/callback`
  - `POST` plaintext XML callbacks with `MsgType=text` are normalized into one `ChannelInboundMessage`
  - non-text callbacks currently acknowledge with `success` and produce no normalized messages

### Current Limits

- inbound processing currently stops after `ChannelAccessPolicy` and `ChannelRouteResolver`; it does not yet create or resume agent runs
- inbound processing currently persists only text-bearing user messages into `SessionManager`
- inbound processing does not yet trigger `AgentRunGateway` automatically after session persistence
- Telegram inbound support currently focuses on text-bearing message updates and callback-query payloads only
- WeCom inbound support currently focuses on verification echo and plaintext XML text callbacks only
- WeCom signature verification, message decryption, and encrypted callback responses are still future work
- Telegram media, WeCom rich-media, and advanced interactive payloads are not yet implemented
