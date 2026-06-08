package com.corwin.system.diagnostic.infrastructure.runtime;

import com.corwin.system.diagnostic.domain.model.DiagnosticItem;
import com.corwin.system.diagnostic.domain.model.DiagnosticSession;
import com.corwin.system.diagnostic.domain.repo.DiagnosticEventRepository;
import com.corwin.system.diagnostic.domain.repo.DiagnosticSessionRepository;
import com.corwin.system.diagnostic.domain.repo.DiagnosticSnapshotRepository;
import com.corwin.system.diagnostic.infrastructure.collector.HttpRequestObserver;
import com.corwin.system.diagnostic.infrastructure.collector.JfrCollector;
import com.corwin.system.diagnostic.infrastructure.collector.SqlExecutionObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Corwin 2026/4/16
 */
@Slf4j
@Component
public class DiagnosticRuntimeManager {

    private final DiagnosticSessionRepository sessionRepository;
    private final DiagnosticSnapshotRepository snapshotRepository;
    private final DiagnosticEventRepository eventRepository;
    private final DiagnosticScheduler diagnosticScheduler;
    private final DiagnosticSnapshotCollector snapshotCollector;
    private final HttpRequestObserver httpRequestObserver;
    private final SqlExecutionObserver sqlExecutionObserver;
    private final JfrCollector jfrCollector;
    private final AtomicReference<RuntimeState> state = new AtomicReference<>();

    public DiagnosticRuntimeManager(DiagnosticSessionRepository sessionRepository,
                                    DiagnosticSnapshotRepository snapshotRepository,
                                    DiagnosticEventRepository eventRepository, DiagnosticScheduler diagnosticScheduler,
                                    DiagnosticSnapshotCollector snapshotCollector,
                                    HttpRequestObserver httpRequestObserver,
                                    SqlExecutionObserver sqlExecutionObserver, JfrCollector jfrCollector) {
        this.sessionRepository = sessionRepository;
        this.snapshotRepository = snapshotRepository;
        this.eventRepository = eventRepository;
        this.diagnosticScheduler = diagnosticScheduler;
        this.snapshotCollector = snapshotCollector;
        this.httpRequestObserver = httpRequestObserver;
        this.sqlExecutionObserver = sqlExecutionObserver;
        this.jfrCollector = jfrCollector;
    }

    public synchronized DiagnosticSession start(DiagnosticSession session) {
        stopInternal(false);
        snapshotRepository.configureCapacity(session.config().historyCapacity());
        eventRepository.configureCapacity(session.config().eventCapacity());
        snapshotRepository.clear();
        eventRepository.clear();
        httpRequestObserver.reset();
        sqlExecutionObserver.reset();
        jfrCollector.reset();
        if (session.config().includes(DiagnosticItem.JFR)) {
            jfrCollector.start(session.config());
        }
        sessionRepository.save(session);
        ScheduledFuture<?> future =
                diagnosticScheduler.scheduleAtFixedRate(() -> captureSnapshot(session), session.config().intervalMs());
        state.set(new RuntimeState(session, future));
        captureSnapshot(session);
        return session;
    }

    public synchronized DiagnosticSession update(DiagnosticSession session) {
        RuntimeState current = state.get();
        if (current != null) {
            diagnosticScheduler.cancel(current.future());
        }
        snapshotRepository.configureCapacity(session.config().historyCapacity());
        eventRepository.configureCapacity(session.config().eventCapacity());
        if (session.config().includes(DiagnosticItem.JFR)) {
            jfrCollector.start(session.config());
        } else {
            jfrCollector.stop();
        }
        sessionRepository.save(session);
        ScheduledFuture<?> future =
                diagnosticScheduler.scheduleAtFixedRate(() -> captureSnapshot(session), session.config().intervalMs());
        state.set(new RuntimeState(session, future));
        captureSnapshot(session);
        return session;
    }

    public synchronized boolean stop() {
        return stopInternal(true);
    }

    public Optional<DiagnosticSession> currentSession() {
        expireIfNecessary();
        return sessionRepository.current().filter(item -> item.isActive(Instant.now()));
    }

    public boolean isCollecting(DiagnosticItem item) {
        RuntimeState current = state.get();
        if (current == null) {
            return false;
        }
        if (current.session().isExpired(Instant.now())) {
            stop();
            return false;
        }
        return current.session().config().includes(item);
    }

    public long slowRequestThresholdMs() {
        return currentSession().map(item -> item.config().slowRequestThresholdMs()).orElse(0L);
    }

    public long slowSqlThresholdMs() {
        return currentSession().map(item -> item.config().slowSqlThresholdMs()).orElse(0L);
    }

    private synchronized boolean stopInternal(boolean clearSessionRepository) {
        RuntimeState current = state.getAndSet(null);
        if (current == null && !clearSessionRepository) {
            return false;
        }
        if (current != null) {
            diagnosticScheduler.cancel(current.future());
        }
        jfrCollector.stop();
        httpRequestObserver.reset();
        sqlExecutionObserver.reset();
        snapshotRepository.clear();
        eventRepository.clear();
        if (clearSessionRepository) {
            sessionRepository.clear();
        }
        return current != null;
    }

    private void expireIfNecessary() {
        RuntimeState current = state.get();
        if (current == null) {
            return;
        }
        if (current.session().isExpired(Instant.now())) {
            log.info("Diagnostic session expired, stop current runtime session");
            stop();
        }
    }

    private void captureSnapshot(DiagnosticSession session) {
        if (!session.isActive(Instant.now())) {
            stop();
            return;
        }
        snapshotRepository.append(snapshotCollector.collect(session.config()));
    }

    private record RuntimeState(
            DiagnosticSession session,
            ScheduledFuture<?> future
    ) {
    }
}
