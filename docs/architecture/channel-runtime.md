# IntentForge Channel Runtime

## Purpose

`intentforge-channel` provides the runtime spine for inbound and outbound messaging channels.
It is designed for pluggable multi-channel integrations such as Telegram and WeCom without pushing vendor-specific logic into the shared core model.

## Module Layout

| Module | Responsibility |
| --- | --- |
| `intentforge-channel-core` | Shared channel descriptors, account/target/message/webhook models, inbound processing contracts, routing contracts, and manager/plugin SPI |
| `intentforge-channel-local` | In-memory `ChannelManager`, inbound processing pipeline, classpath plugin loading, and external plugin directory loading |
| `intentforge-channel-spring` | Spring `spring.factories` discovery bridge that contributes `ChannelPlugin` instances through a dedicated SPI strategy |
| `intentforge-channel-connectors` | Loopback and generic connector support entrypoints |
| `intentforge-channel-telegram` | Telegram Bot API outbound connector |
| `intentforge-channel-wecom` | WeCom application messaging outbound connector |

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

`DefaultChannelInboundProcessor` in `intentforge-channel-local` chains:

- `ChannelManager.openWebhookHandler(accountProfile)`
- ordered `ChannelAccessPolicy` implementations loaded from classpath `ServiceLoader`
- ordered `ChannelRouteResolver` implementations loaded from classpath `ServiceLoader`

Fallback behavior:

- when no access policy is discovered, the processor uses an allow-all policy
- when no route resolver matches, the local default route resolver maps:
  - `spaceId` -> `ChannelInboundMessage.accountId()`
  - `sessionId` -> `<channelType>:<conversationId>[:<threadId>]`
  - `agentId` -> optional `metadata.agentId`

`DirectoryChannelPluginManager` mirrors the existing plugin runtime pattern and loads external channel plugin jars from `plugins/`.

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

The local runtime also exposes one `ChannelInboundProcessor` that executes:

- webhook normalization inside the selected connector module
- access-policy evaluation
- route resolution with fallback behavior

The channel manager is currently bootstrap-scoped, similar to the session manager.

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
- current scope:
  - outbound text delivery via Telegram Bot API `sendMessage`
  - inbound webhook normalization for text-bearing updates
- required account properties:
  - `botToken`: Telegram bot token
  - `baseUrl`: optional Bot API base URL, defaults to `https://api.telegram.org`
- target mapping:
  - `ChannelTarget.conversationId` -> `chat_id`
  - `ChannelTarget.threadId` -> `message_thread_id`
- outbound metadata mapping:
  - `parseMode` -> `parse_mode`
  - `disableNotification` -> `disable_notification`
  - `disableWebPagePreview` -> `link_preview_options.is_disabled`
- inbound webhook behavior:
  - `POST` JSON updates are parsed through `ChannelWebhookHandler`
  - `message`, `edited_message`, `channel_post`, and `edited_channel_post` with `text` are normalized into one `ChannelInboundMessage`
  - non-text updates currently acknowledge with `200 OK` and produce no normalized messages

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
  - `POST` plaintext XML callbacks with `MsgType=text` are normalized into one `ChannelInboundMessage`
  - non-text callbacks currently acknowledge with `success` and produce no normalized messages

### Current Limits

- inbound parsing currently stops at connector-local normalization and does not yet invoke `ChannelAccessPolicy` or `ChannelRouteResolver`
- inbound processing currently stops after `ChannelAccessPolicy` and `ChannelRouteResolver`; it does not yet create or resume agent runs
- inbound processing does not yet persist routed user messages into `SessionManager`
- Telegram inbound support currently focuses on text-bearing updates only
- WeCom inbound support currently focuses on verification echo and plaintext XML text callbacks only
- WeCom signature verification, message decryption, and encrypted callback responses are still future work
- Telegram media, WeCom rich-media, and advanced interactive payloads are not yet implemented
