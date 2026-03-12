package cn.intentforge.session.registry;

import cn.intentforge.session.model.Session;
import cn.intentforge.session.model.SessionDraft;
import cn.intentforge.session.model.SessionMessageDraft;
import cn.intentforge.session.model.SessionQuery;
import java.util.List;
import java.util.Optional;

/**
 * Manages session lifecycle, message history, and session queries.
 *
 * <p>Implementations should document their thread-safety guarantees.
 */
public interface SessionManager {
  /**
   * Creates and stores a new session.
   *
   * @param draft session creation input
   * @return created session snapshot
   * @throws IllegalStateException when a session with the same identifier already exists
   */
  Session create(SessionDraft draft);

  /**
   * Finds one session by identifier.
   *
   * @param id session identifier
   * @return stored session snapshot when present
   */
  Optional<Session> find(String id);

  /**
   * Lists sessions matching the supplied query.
   *
   * @param query query filter; {@code null} means all sessions
   * @return matching immutable session snapshots
   */
  List<Session> list(SessionQuery query);

  /**
   * Appends a message to an existing session.
   *
   * @param sessionId target session identifier
   * @param messageDraft message creation input
   * @return updated session snapshot
   */
  Session appendMessage(String sessionId, SessionMessageDraft messageDraft);

  /**
   * Archives an existing session.
   *
   * @param sessionId target session identifier
   * @return updated archived session snapshot
   */
  Session archive(String sessionId);

  /**
   * Deletes one session when the identifier is present.
   *
   * @param sessionId session identifier
   */
  void delete(String sessionId);
}
