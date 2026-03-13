# IntentForge Plugin Runtime

## Layout

External plugins are loaded from a local `plugins/` directory.

```text
plugins/
├─ openai-provider.jar
└─ openai-provider.properties
```

- `*.jar`: plugin artifact
- `*.properties`: optional sidecar override file, used for enable/disable or metadata override

## Required files inside plugin jar

Each plugin jar must include:

1. `META-INF/services/<spi-interface>`
2. `META-INF/intentforge-plugin.properties`

Examples:

- prompt plugin SPI: `META-INF/services/cn.intentforge.prompt.spi.PromptPlugin`
- model plugin SPI: `META-INF/services/cn.intentforge.model.spi.ModelPlugin`
- model-provider plugin SPI: `META-INF/services/cn.intentforge.model.provider.spi.ModelProviderPlugin`

## Manager SPI (classpath)

In addition to source plugins, manager/registry implementations can be replaced via classpath SPI:

- prompt manager SPI: `META-INF/services/cn.intentforge.prompt.spi.PromptManagerProvider`
- model manager SPI: `META-INF/services/cn.intentforge.model.spi.ModelManagerProvider`
- model-provider registry SPI: `META-INF/services/cn.intentforge.model.provider.spi.ModelProviderRegistryProvider`
- session manager SPI: `META-INF/services/cn.intentforge.session.spi.SessionManagerProvider`

Selection rule:

- SPI only declares which implementations are available
- bootstrap instantiates discovered implementations and builds `RuntimeCatalog`
- each `RuntimeImplementationDescriptor` exposes explicit `version` metadata for runtime observability and API responses
- `SpaceProfile.runtimeBindings` selects the effective prompt/model/provider/tool runtime per run
- if a capability has only one implementation, or one implementation marked with metadata `default=true`, it can be used as the default fallback when the space does not bind it explicitly
- bootstrap-scoped capabilities such as the session manager still resolve once at bootstrap and are exposed in run observability data

`session` currently uses classpath manager SPI only and is assembled via
`cn.intentforge.session.local.SessionLocalRuntimeFactory`.

## Plugin metadata

`META-INF/intentforge-plugin.properties` supports:

```properties
plugin.id=openai-provider
plugin.name=OpenAI Provider
plugin.version=1.0.0
plugin.description=OpenAI compatible model provider plugin
plugin.enabled=true
plugin.apiVersion=1
plugin.intentforgeVersion=nightly-SNAPSHOT
```

Rules:

- `plugin.id`: required
- `plugin.apiVersion`: required, must match runtime API version
- `plugin.intentforgeVersion`: optional, `*` or exact runtime version
- `plugin.enabled`: defaults to `true`

## Sidecar override

Optional sidecar file path:

```text
plugins/openai-provider.properties
```

Sidecar properties override the values packaged inside the jar.

Example:

```properties
plugin.enabled=false
```

## Runtime entrypoints

- prompt: `cn.intentforge.prompt.local.plugin.DirectoryPromptPluginManager`
- model: `cn.intentforge.model.local.plugin.DirectoryModelPluginManager`
- model-provider: `cn.intentforge.model.provider.local.plugin.DirectoryModelProviderPluginManager`

Supported operations:

- `loadAll()`: scan `plugins/` directory and load all plugins
- `start(pluginId)`: start or reload one plugin
- `stop(pluginId)`: stop one plugin and unregister its contributions
- `plugins()`: list current plugin handles, metadata, state, and message

Default local bootstrap:

- `cn.intentforge.boot.local.AiAssetLocalBootstrap.bootstrap()`
- default plugin directory: `./plugins`

## Validation and states

Validation is handled by `cn.intentforge.common.plugin.DirectoryServicePluginLoader`.

Possible states:

- `ACTIVE`
- `DISABLED`
- `REJECTED`
- `STOPPED`

Rejected reasons include:

- missing `META-INF/intentforge-plugin.properties`
- missing `plugin.apiVersion`
- incompatible `plugin.apiVersion`
- incompatible `plugin.intentforgeVersion`
- no SPI implementation found in `META-INF/services/...`
