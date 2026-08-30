package com.corwin.system.diagnostic.infrastructure.runtime;

import com.corwin.system.diagnostic.domain.model.DiagnosticSnapshot;
import com.corwin.system.diagnostic.domain.repo.DiagnosticSnapshotRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * In-memory implementation of DiagnosticSnapshotRepository backed by a DiagnosticRingBuffer.
 *
 * @author Corwin 2026/4/16
 */
@Repository
public class InMemoryDiagnosticSnapshotRepository implements DiagnosticSnapshotRepository {

  private final DiagnosticRingBuffer<DiagnosticSnapshot> buffer =
      new DiagnosticRingBuffer<>(
          com.corwin.system.diagnostic.domain.model.DiagnosticConfig.DEFAULT_HISTORY_CAPACITY);

  @Override
  public void configureCapacity(int capacity) {
    buffer.configureCapacity(capacity);
  }

  @Override
  public void append(DiagnosticSnapshot snapshot) {
    buffer.add(snapshot);
  }

  @Override
  public Optional<DiagnosticSnapshot> latest() {
    return Optional.ofNullable(buffer.latest());
  }

  @Override
  public List<DiagnosticSnapshot> latest(int limit) {
    return buffer.latest(limit);
  }

  @Override
  public void clear() {
    buffer.clear();
  }
}
