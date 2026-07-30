package com.corwin.system.diagnostic.application.service;

import com.corwin.framework.util.HighDate;
import com.corwin.system.diagnostic.application.view.DiagnosticCapabilityView;
import com.corwin.system.diagnostic.application.view.DiagnosticSessionView;
import com.corwin.system.diagnostic.domain.model.DiagnosticConfig;
import com.corwin.system.diagnostic.domain.model.DiagnosticEvent;
import com.corwin.system.diagnostic.domain.model.DiagnosticEventType;
import com.corwin.system.diagnostic.domain.model.DiagnosticSnapshot;
import com.corwin.system.diagnostic.domain.model.DiagnosticStatus;
import com.corwin.system.diagnostic.domain.repo.DiagnosticEventRepository;
import com.corwin.system.diagnostic.domain.repo.DiagnosticSnapshotRepository;
import com.corwin.system.diagnostic.infrastructure.collector.DbPoolStateCollector;
import com.corwin.system.diagnostic.infrastructure.collector.JfrCollector;
import com.corwin.system.diagnostic.infrastructure.runtime.DiagnosticRuntimeManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Application service for diagnostic query operations: retrieving session status,
 * snapshots, events, and runtime capabilities.
 *
 * @author Corwin 2026/4/16
 */
@Service
public class DiagnosticQueryAppService {

    private final DiagnosticRuntimeManager runtimeManager;
    private final DiagnosticSnapshotRepository snapshotRepository;
    private final DiagnosticEventRepository eventRepository;
    private final DbPoolStateCollector dbPoolStateCollector;
    private final JfrCollector jfrCollector;

    public DiagnosticQueryAppService(DiagnosticRuntimeManager runtimeManager,
                                     DiagnosticSnapshotRepository snapshotRepository,
                                     DiagnosticEventRepository eventRepository,
                                     DbPoolStateCollector dbPoolStateCollector, JfrCollector jfrCollector) {
        this.runtimeManager = runtimeManager;
        this.snapshotRepository = snapshotRepository;
        this.eventRepository = eventRepository;
        this.dbPoolStateCollector = dbPoolStateCollector;
        this.jfrCollector = jfrCollector;
    }

    /**
     * Returns the current diagnostic session status, or an INACTIVE status if no session is active.
     *
     * @return the diagnostic session view
     */
    public DiagnosticSessionView status() {
        Instant now = HighDate.realInstant();
        return runtimeManager.currentSession()
                .map(session -> new DiagnosticSessionView(session.status(), session.config(), session.startedAt(),
                        session.expireAt(), session.remainingTtlSeconds(now)))
                .orElseGet(() -> new DiagnosticSessionView(DiagnosticStatus.INACTIVE, DiagnosticConfig.defaultConfig(),
                        null, null, 0L));
    }

    /**
     * Retrieves the most recent diagnostic snapshot.
     *
     * @return an Optional containing the latest snapshot, or empty if none exists
     */
    public Optional<DiagnosticSnapshot> latestSnapshot() {
        runtimeManager.currentSession();
        return snapshotRepository.latest();
    }

    /**
     * Retrieves a list of historical diagnostic snapshots, up to the specified limit.
     *
     * @param limit the maximum number of snapshots to return
     * @return a list of recent diagnostic snapshots
     */
    public List<DiagnosticSnapshot> history(int limit) {
        runtimeManager.currentSession();
        return snapshotRepository.latest(normalizeLimit(limit, 120));
    }

    /**
     * Retrieves diagnostic events, optionally filtered by event type.
     *
     * @param limit the maximum number of events to return
     * @param type  the event type filter, or null for all events
     * @return a list of matching diagnostic events
     */
    public List<DiagnosticEvent> events(int limit, DiagnosticEventType type) {
        runtimeManager.currentSession();
        List<DiagnosticEvent> latestEvents = eventRepository.latest(normalizeLimit(limit, 100) * 4);
        if (type == null) {
            return latestEvents.stream().limit(normalizeLimit(limit, 100)).toList();
        }
        return latestEvents.stream().filter(event -> event.type() == type).limit(normalizeLimit(limit, 100)).toList();
    }

    /**
     * Returns the diagnostic capabilities available in the current runtime environment,
     * including JFR, HTTP, connection pool, and SQL monitoring availability.
     *
     * @return the diagnostic capability view
     */
    public DiagnosticCapabilityView capabilities() {
        return new DiagnosticCapabilityView(jfrCollector.isAvailable(), true, dbPoolStateCollector.hasRegisteredDataSource(),
                dbPoolStateCollector.hasRegisteredDataSource(), true, dbPoolStateCollector.dataSourceNames());
    }

    private int normalizeLimit(int limit, int defaultValue) {
        if (limit <= 0) {
            return defaultValue;
        }
        return Math.min(limit, 500);
    }
}
