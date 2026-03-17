package cn.intentforge.hook;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelDriver;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookAdministration;
import cn.intentforge.channel.ChannelWebhookDeletion;
import cn.intentforge.channel.ChannelWebhookRegistration;
import cn.intentforge.channel.ChannelWebhookStatus;
import cn.intentforge.channel.registry.ChannelManager;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HookWebhookAutoManagerTest {
  @Test
  void shouldRegisterManagedTelegramWebhookWithExplicitBaseUrl() {
    RecordingWebhookAdministration administration = new RecordingWebhookAdministration();
    InMemoryHookAccountRegistry registry = new InMemoryHookAccountRegistry();
    registry.register(new ChannelAccountProfile(
        "telegram-account",
        ChannelType.TELEGRAM,
        "Telegram Bot",
        Map.of(
            "botToken", "bot-token",
            "webhookAutoManage", "true",
            "webhookBaseUrl", "https://hooks.example.com",
            "webhookSecretToken", "secret-token",
            "webhookAllowedUpdates", "message, callback_query",
            "webhookMaxConnections", "42",
            "webhookDropPendingUpdates", "true")));

    HookWebhookAutoManager manager = new HookWebhookAutoManager(registry, new FixedChannelManager(administration));
    List<HookWebhookAutoManagementResult> results = manager.reconcile(URI.create("http://127.0.0.1:8080"));

    Assertions.assertEquals(1, results.size());
    Assertions.assertEquals(HookWebhookAutoManagementOperation.REGISTER, results.getFirst().operation());
    Assertions.assertEquals(
        URI.create("https://hooks.example.com/open-api/hooks/telegram/accounts/telegram-account/webhook"),
        administration.registration.url());
    Assertions.assertEquals("secret-token", administration.registration.secretToken());
    Assertions.assertEquals(List.of("message", "callback_query"), administration.registration.eventTypes());
    Assertions.assertEquals(42, administration.registration.maxConnections());
    Assertions.assertTrue(administration.registration.dropPendingUpdates());
    Assertions.assertEquals(1, administration.getWebhookInfoCalls);
  }

  @Test
  void shouldDeleteManagedWebhookWhenDesiredStateIsUnregistered() {
    RecordingWebhookAdministration administration = new RecordingWebhookAdministration();
    InMemoryHookAccountRegistry registry = new InMemoryHookAccountRegistry();
    registry.register(new ChannelAccountProfile(
        "telegram-account",
        ChannelType.TELEGRAM,
        "Telegram Bot",
        Map.of(
            "botToken", "bot-token",
            "webhookAutoManage", "true",
            "webhookDesiredState", "UNREGISTERED",
            "webhookDropPendingUpdates", "true")));

    HookWebhookAutoManager manager = new HookWebhookAutoManager(registry, new FixedChannelManager(administration));
    List<HookWebhookAutoManagementResult> results = manager.reconcile(URI.create("http://127.0.0.1:8080"));

    Assertions.assertEquals(1, results.size());
    Assertions.assertEquals(HookWebhookAutoManagementOperation.DELETE, results.getFirst().operation());
    Assertions.assertNotNull(administration.deletion);
    Assertions.assertTrue(administration.deletion.dropPendingUpdates());
    Assertions.assertNull(administration.registration);
    Assertions.assertEquals(1, administration.getWebhookInfoCalls);
  }

  private static final class FixedChannelManager implements ChannelManager {
    private final ChannelWebhookAdministration administration;

    private FixedChannelManager(ChannelWebhookAdministration administration) {
      this.administration = administration;
    }

    @Override
    public void register(ChannelDriver driver) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void registerAll(Collection<ChannelDriver> drivers) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void unregister(String driverId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ChannelDriver> find(String driverId) {
      return Optional.empty();
    }

    @Override
    public Optional<ChannelDriver> findDriver(ChannelAccountProfile accountProfile) {
      return Optional.of(new FixedChannelDriver(administration, accountProfile.type()));
    }

    @Override
    public Optional<cn.intentforge.channel.ChannelSession> openSession(ChannelAccountProfile accountProfile) {
      return Optional.empty();
    }

    @Override
    public Optional<cn.intentforge.channel.ChannelWebhookHandler> openWebhookHandler(ChannelAccountProfile accountProfile) {
      return Optional.empty();
    }

    @Override
    public Optional<ChannelWebhookAdministration> openWebhookAdministration(ChannelAccountProfile accountProfile) {
      return Optional.of(administration);
    }

    @Override
    public List<ChannelDriver> list() {
      return List.of();
    }

    @Override
    public void loadPlugins() {
    }
  }

  private static final class FixedChannelDriver implements ChannelDriver {
    private final ChannelWebhookAdministration administration;
    private final ChannelType channelType;

    private FixedChannelDriver(ChannelWebhookAdministration administration, ChannelType channelType) {
      this.administration = administration;
      this.channelType = channelType;
    }

    @Override
    public cn.intentforge.channel.ChannelDescriptor descriptor() {
      return new cn.intentforge.channel.ChannelDescriptor(
          "test-driver",
          channelType,
          "Test Driver",
          List.of(),
          Map.of());
    }

    @Override
    public cn.intentforge.channel.ChannelSession openSession(ChannelAccountProfile accountProfile) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ChannelWebhookAdministration> openWebhookAdministration(ChannelAccountProfile accountProfile) {
      return Optional.of(administration);
    }
  }

  private static final class RecordingWebhookAdministration implements ChannelWebhookAdministration {
    private ChannelWebhookRegistration registration;
    private ChannelWebhookDeletion deletion;
    private int getWebhookInfoCalls;

    @Override
    public void setWebhook(ChannelWebhookRegistration registration) {
      this.registration = registration;
    }

    @Override
    public void deleteWebhook(ChannelWebhookDeletion deletion) {
      this.deletion = deletion;
    }

    @Override
    public Optional<ChannelWebhookStatus> getWebhookInfo() {
      getWebhookInfoCalls++;
      return Optional.of(new ChannelWebhookStatus(URI.create("https://hooks.example.com/demo"), Map.of()));
    }
  }
}
