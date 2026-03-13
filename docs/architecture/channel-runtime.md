# IntentForge Channel Runtime

## Purpose

`intentforge-channel` provides the runtime spine for inbound and outbound messaging channels.
It is designed for pluggable multi-channel integrations such as Telegram and WeCom without pushing vendor-specific logic into the shared core model.

## Module Layout

| Module | Responsibility |
| --- | --- |
| `intentforge-channel-core` | Shared channel descriptors, account/target/message models, routing contracts, and manager/plugin SPI |
| `intentforge-channel-local` | In-memory `ChannelManager`, classpath plugin loading, and external plugin directory loading |
| `intentforge-channel-spring` | Spring `spring.factories` discovery bridge that contributes `ChannelPlugin` instances through a dedicated SPI strategy |
| `intentforge-channel-connectors` | Built-in connector implementations and reusable connector entrypoints |

## Core Model

The shared runtime model is intentionally transport-neutral:

- `ChannelDescriptor`: describes one driver and its normalized capabilities
- `ChannelAccountProfile`: describes one configured account instance
- `ChannelTarget`: represents a normalized conversation/thread/recipient target
- `ChannelInboundMessage`: normalized inbound event payload
- `ChannelOutboundRequest`: normalized outbound send request
- `ChannelDeliveryResult`: normalized send result
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

The channel manager is currently bootstrap-scoped, similar to the session manager.

## Future Connector Guidance

Telegram and WeCom adapters should follow these rules:

- keep vendor DTOs and API clients inside connector modules
- map vendor events into `ChannelInboundMessage`
- map outbound send requests from `ChannelOutboundRequest`
- use `ChannelAccountProfile.properties` and request metadata for connector-specific settings
- keep routing and access control in shared contracts instead of hard-coded connector logic
