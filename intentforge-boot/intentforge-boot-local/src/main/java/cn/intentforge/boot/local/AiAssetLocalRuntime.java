package cn.intentforge.boot.local;

import cn.intentforge.model.local.plugin.DirectoryModelPluginManager;
import cn.intentforge.model.provider.local.plugin.DirectoryModelProviderPluginManager;
import cn.intentforge.model.provider.registry.ModelProviderRegistry;
import cn.intentforge.model.registry.ModelManager;
import cn.intentforge.prompt.local.plugin.DirectoryPromptPluginManager;
import cn.intentforge.prompt.registry.PromptManager;

/**
 * Local runtime wiring for prompt, model, and provider assets.
 *
 * @param promptManager prompt manager implementation in use
 * @param promptPluginManager prompt plugin directory manager
 * @param modelManager model manager implementation in use
 * @param modelPluginManager model plugin directory manager
 * @param providerRegistry model provider registry implementation in use
 * @param providerPluginManager model provider plugin directory manager
 */
public record AiAssetLocalRuntime(
    PromptManager promptManager,
    DirectoryPromptPluginManager promptPluginManager,
    ModelManager modelManager,
    DirectoryModelPluginManager modelPluginManager,
    ModelProviderRegistry providerRegistry,
    DirectoryModelProviderPluginManager providerPluginManager
) {
}
