package com.corwin.system.diagnostic.infrastructure.runtime;

import com.corwin.system.diagnostic.domain.model.DiagnosticConfig;
import com.corwin.system.diagnostic.domain.model.DiagnosticEvent;
import com.corwin.system.diagnostic.domain.repo.DiagnosticEventRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Corwin 2026/4/16
 */
@Repository
public class InMemoryDiagnosticEventRepository implements DiagnosticEventRepository {

    private final DiagnosticRingBuffer<DiagnosticEvent> buffer =
            new DiagnosticRingBuffer<>(DiagnosticConfig.DEFAULT_EVENT_CAPACITY);

    @Override
    public void configureCapacity(int capacity) {
        buffer.configureCapacity(capacity);
    }

    @Override
    public void append(DiagnosticEvent event) {
        buffer.add(event);
    }

    @Override
    public List<DiagnosticEvent> latest(int limit) {
        return buffer.latest(limit);
    }

    @Override
    public void clear() {
        buffer.clear();
    }
}
