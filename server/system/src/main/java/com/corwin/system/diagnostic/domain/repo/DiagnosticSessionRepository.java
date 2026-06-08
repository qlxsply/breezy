package com.corwin.system.diagnostic.domain.repo;

import com.corwin.system.diagnostic.domain.model.DiagnosticSession;

import java.util.Optional;

/**
 * @author Corwin 2026/4/16
 */
public interface DiagnosticSessionRepository {

    Optional<DiagnosticSession> current();

    DiagnosticSession save(DiagnosticSession session);

    void clear();
}
