package com.corwin.system.diagnostic.infrastructure.runtime;

import com.corwin.system.diagnostic.domain.model.DiagnosticSession;
import com.corwin.system.diagnostic.domain.repo.DiagnosticSessionRepository;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Repository;

/**
 * In-memory implementation of DiagnosticSessionRepository backed by an AtomicReference.
 *
 * @author Corwin 2026/4/16
 */
@Repository
public class InMemoryDiagnosticSessionRepository implements DiagnosticSessionRepository {

  private final AtomicReference<DiagnosticSession> current = new AtomicReference<>();

  @Override
  public Optional<DiagnosticSession> current() {
    return Optional.ofNullable(current.get());
  }

  @Override
  public DiagnosticSession save(DiagnosticSession session) {
    current.set(session);
    return session;
  }

  @Override
  public void clear() {
    current.set(null);
  }
}
