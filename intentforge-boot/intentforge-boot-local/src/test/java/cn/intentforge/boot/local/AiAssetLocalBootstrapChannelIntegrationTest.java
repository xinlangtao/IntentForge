package cn.intentforge.boot.local;

import cn.intentforge.config.RuntimeCapability;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AiAssetLocalBootstrapChannelIntegrationTest {
  @Test
  void shouldExposeBuiltinChannelRuntimeAndClasspathDrivers() throws Exception {
    AiAssetLocalRuntime runtime = AiAssetLocalBootstrap.bootstrap(Files.createTempDirectory("boot-channel-plugins"));

    Assertions.assertEquals(
        List.of("intentforge.channel.manager.in-memory"),
        runtime.runtimeCatalog().list(RuntimeCapability.CHANNEL_MANAGER).stream().map(descriptor -> descriptor.id()).toList());
    Assertions.assertTrue(runtime.channelManager().find("intentforge.channel.loopback").isPresent());
  }
}
