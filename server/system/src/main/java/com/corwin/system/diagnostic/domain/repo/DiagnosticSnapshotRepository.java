package com.corwin.system.diagnostic.domain.repo;

import com.corwin.system.diagnostic.domain.model.DiagnosticSnapshot;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/4/16
 */
public interface DiagnosticSnapshotRepository {

    void configureCapacity(int capacity);

    void append(DiagnosticSnapshot snapshot);

    Optional<DiagnosticSnapshot> latest();

    List<DiagnosticSnapshot> latest(int limit);

    void clear();
}
