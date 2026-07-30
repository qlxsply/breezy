package com.corwin.system.diagnostic.domain.repo;

import com.corwin.system.diagnostic.domain.model.DiagnosticSession;

import java.util.Optional;

/**
 * Repository interface for persisting and retrieving the current diagnostic session.
 *
 * @author Corwin 2026/4/16
 */
public interface DiagnosticSessionRepository {

    /**
     * Returns the current diagnostic session, if one exists.
     *
     * @return the current session wrapped in Optional
     */
    Optional<DiagnosticSession> current();

    /**
     * Saves the given session, replacing any existing current session.
     *
     * @param session the session to save
     * @return the saved session
     */
    DiagnosticSession save(DiagnosticSession session);

    /**
     * Clears the current session, setting it to null.
     */
    void clear();
}
