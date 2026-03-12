package cn.intentforge.session.local;

import cn.intentforge.session.registry.SessionManager;

/**
 * Local runtime wiring for session management.
 *
 * @param sessionManager session manager implementation in use
 */
public record SessionLocalRuntime(
    SessionManager sessionManager
) {
}
