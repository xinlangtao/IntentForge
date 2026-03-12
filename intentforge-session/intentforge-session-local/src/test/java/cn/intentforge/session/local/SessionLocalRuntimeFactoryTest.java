package cn.intentforge.session.local;

import cn.intentforge.session.local.registry.InMemorySessionManager;
import cn.intentforge.session.model.Session;
import cn.intentforge.session.model.SessionDraft;
import cn.intentforge.session.model.SessionMessageDraft;
import cn.intentforge.session.model.SessionQuery;
import cn.intentforge.session.registry.SessionManager;
import cn.intentforge.session.spi.SessionManagerProvider;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SessionLocalRuntimeFactoryTest {
  @Test
  void shouldCreateRuntimeFromDefaultProvider() {
    SessionLocalRuntime runtime = SessionLocalRuntimeFactory.create();

    Assertions.assertNotNull(runtime.sessionManager());
    Assertions.assertTrue(runtime.sessionManager() instanceof InMemorySessionManager);
  }

  @Test
  void shouldPreferHigherPriorityProvider() {
    SessionManager selected = SessionLocalRuntimeFactory.selectSingleProvider(
        List.of(new LowPriorityProvider(), new HighPriorityProvider()),
        SessionManagerProvider.class,
        SessionManagerProvider::priority,
        provider -> provider.create(Thread.currentThread().getContextClassLoader()),
        InMemorySessionManager::new);

    Assertions.assertTrue(selected instanceof HighPrioritySessionManager);
  }

  @Test
  void shouldRejectDuplicateHighestPriorityProviders() {
    IllegalStateException exception = Assertions.assertThrows(
        IllegalStateException.class,
        () -> SessionLocalRuntimeFactory.selectSingleProvider(
            List.of(new HighPriorityProvider(), new AnotherHighPriorityProvider()),
            SessionManagerProvider.class,
            SessionManagerProvider::priority,
            provider -> provider.create(Thread.currentThread().getContextClassLoader()),
            InMemorySessionManager::new));

    Assertions.assertTrue(exception.getMessage().contains("SessionManagerProvider"));
    Assertions.assertTrue(exception.getMessage().contains("priority 10"));
  }

  private static final class LowPriorityProvider implements SessionManagerProvider {
    @Override
    public int priority() {
      return 0;
    }

    @Override
    public SessionManager create(ClassLoader classLoader) {
      return new InMemorySessionManager(classLoader);
    }
  }

  private static final class HighPriorityProvider implements SessionManagerProvider {
    @Override
    public int priority() {
      return 10;
    }

    @Override
    public SessionManager create(ClassLoader classLoader) {
      return new HighPrioritySessionManager();
    }
  }

  private static final class AnotherHighPriorityProvider implements SessionManagerProvider {
    @Override
    public int priority() {
      return 10;
    }

    @Override
    public SessionManager create(ClassLoader classLoader) {
      return new HighPrioritySessionManager();
    }
  }

  private static final class HighPrioritySessionManager implements SessionManager {
    @Override
    public Session create(SessionDraft draft) {
      throw new UnsupportedOperationException("not used in this test");
    }

    @Override
    public Optional<Session> find(String id) {
      return Optional.empty();
    }

    @Override
    public List<Session> list(SessionQuery query) {
      return List.of();
    }

    @Override
    public Session appendMessage(String sessionId, SessionMessageDraft messageDraft) {
      throw new UnsupportedOperationException("not used in this test");
    }

    @Override
    public Session archive(String sessionId) {
      throw new UnsupportedOperationException("not used in this test");
    }

    @Override
    public void delete(String sessionId) {
    }
  }
}
