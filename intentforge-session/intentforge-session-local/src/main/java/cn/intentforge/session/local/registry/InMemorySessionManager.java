package cn.intentforge.session.local.registry;

import cn.intentforge.session.SessionNotFoundException;
import cn.intentforge.session.model.Session;
import cn.intentforge.session.model.SessionDraft;
import cn.intentforge.session.model.SessionMessage;
import cn.intentforge.session.model.SessionMessageDraft;
import cn.intentforge.session.model.SessionQuery;
import cn.intentforge.session.model.SessionStatus;
import cn.intentforge.session.registry.SessionManager;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory {@link SessionManager} implementation.
 *
 * <p>This class is thread-safe through synchronized state mutation.
 */
public class InMemorySessionManager implements SessionManager {
  private final ClassLoader classLoader;
  private final Clock clock;
  private final Map<String, Session> sessionsById = new LinkedHashMap<>();

  /**
   * Creates the manager with the current thread context class loader and system clock.
   */
  public InMemorySessionManager() {
    this(Thread.currentThread().getContextClassLoader(), Clock.systemUTC());
  }

  /**
   * Creates the manager with the provided class loader and system clock.
   *
   * @param classLoader class loader associated with the runtime
   */
  public InMemorySessionManager(ClassLoader classLoader) {
    this(classLoader, Clock.systemUTC());
  }

  /**
   * Creates the manager with the provided class loader and clock.
   *
   * @param classLoader class loader associated with the runtime
   * @param clock clock used to stamp created and updated timestamps
   */
  public InMemorySessionManager(ClassLoader classLoader, Clock clock) {
    this.classLoader = classLoader == null ? InMemorySessionManager.class.getClassLoader() : classLoader;
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  /**
   * Creates and stores a new session.
   *
   * @param draft session creation input
   * @return created session snapshot
   */
  @Override
  public synchronized Session create(SessionDraft draft) {
    SessionDraft nonNullDraft = Objects.requireNonNull(draft, "draft must not be null");
    if (sessionsById.containsKey(nonNullDraft.id())) {
      throw new IllegalStateException("session already exists: " + nonNullDraft.id());
    }
    Instant timestamp = Instant.now(clock);
    Session session = new Session(
        nonNullDraft.id(),
        nonNullDraft.title(),
        nonNullDraft.spaceId(),
        SessionStatus.ACTIVE,
        List.of(),
        nonNullDraft.metadata(),
        timestamp,
        timestamp);
    sessionsById.put(session.id(), session);
    return session;
  }

  /**
   * Finds one session by identifier.
   *
   * @param id session identifier
   * @return stored session snapshot when present
   */
  @Override
  public synchronized Optional<Session> find(String id) {
    String normalizedId = normalize(id);
    if (normalizedId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(sessionsById.get(normalizedId));
  }

  /**
   * Lists sessions matching the supplied query.
   *
   * @param query query filter; {@code null} means all sessions
   * @return matching immutable session snapshots
   */
  @Override
  public synchronized List<Session> list(SessionQuery query) {
    List<Session> sessions = new ArrayList<>();
    for (Session session : sessionsById.values()) {
      if (session.matches(query)) {
        sessions.add(session);
      }
    }
    return List.copyOf(sessions);
  }

  /**
   * Appends a new message to an existing session.
   *
   * @param sessionId target session identifier
   * @param messageDraft message creation input
   * @return updated session snapshot
   */
  @Override
  public synchronized Session appendMessage(String sessionId, SessionMessageDraft messageDraft) {
    String normalizedSessionId = requireSessionId(sessionId);
    Session existing = requireSession(normalizedSessionId);
    SessionMessageDraft nonNullDraft = Objects.requireNonNull(messageDraft, "messageDraft must not be null");
    Instant timestamp = Instant.now(clock);
    SessionMessage message = new SessionMessage(
        nonNullDraft.id(),
        nonNullDraft.role(),
        nonNullDraft.content(),
        nonNullDraft.metadata(),
        timestamp);
    List<SessionMessage> messages = new ArrayList<>(existing.messages());
    messages.add(message);
    Session updated = new Session(
        existing.id(),
        existing.title(),
        existing.spaceId(),
        existing.status(),
        messages,
        existing.metadata(),
        existing.createdAt(),
        timestamp);
    sessionsById.put(updated.id(), updated);
    return updated;
  }

  /**
   * Archives an existing session.
   *
   * @param sessionId target session identifier
   * @return updated archived session snapshot
   */
  @Override
  public synchronized Session archive(String sessionId) {
    String normalizedSessionId = requireSessionId(sessionId);
    Session existing = requireSession(normalizedSessionId);
    if (existing.status() == SessionStatus.ARCHIVED) {
      return existing;
    }
    Instant timestamp = Instant.now(clock);
    Session updated = new Session(
        existing.id(),
        existing.title(),
        existing.spaceId(),
        SessionStatus.ARCHIVED,
        existing.messages(),
        existing.metadata(),
        existing.createdAt(),
        timestamp);
    sessionsById.put(updated.id(), updated);
    return updated;
  }

  /**
   * Deletes one session when the identifier is present.
   *
   * @param sessionId session identifier
   */
  @Override
  public synchronized void delete(String sessionId) {
    String normalizedSessionId = normalize(sessionId);
    if (normalizedSessionId == null) {
      return;
    }
    sessionsById.remove(normalizedSessionId);
  }

  /**
   * Returns the runtime class loader associated with this manager.
   *
   * @return runtime class loader
   */
  public ClassLoader classLoader() {
    return classLoader;
  }

  private Session requireSession(String sessionId) {
    Session session = sessionsById.get(sessionId);
    if (session == null) {
      throw new SessionNotFoundException("session not found: " + sessionId);
    }
    return session;
  }

  private static String requireSessionId(String sessionId) {
    String normalized = normalize(sessionId);
    if (normalized == null) {
      throw new IllegalArgumentException("sessionId must not be blank");
    }
    return normalized;
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
