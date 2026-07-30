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
 * Central manager for the diagnostic runtime lifecycle. Orchestrates session start/stop,
 * snapshot scheduling, collector coordination, and automatic expiry handling.
 *
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

    /**
     * Starts a new diagnostic session: configures repositories, clears previous state,
     * starts JFR if enabled, and begins periodic snapshot capture.
     *
     * @param session the session to start
     * @return the started session
     */
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

    /**
     * Updates an existing diagnostic session with new config: reschedules snapshot tasks
     * and restarts JFR if the configuration changed.
     *
     * @param session the updated session
     * @return the updated session
     */
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

    /**
     * Stops the diagnostic session and clears all runtime state.
     *
     * @return true if a session was active and stopped
     */
    public synchronized boolean stop() {
        return stopInternal(true);
    }

    /**
     * Returns the current session if it exists and is still active (not expired).
     *
     * @return the current active session wrapped in Optional
     */
    public Optional<DiagnosticSession> currentSession() {
        expireIfNecessary();
        return sessionRepository.current().filter(item -> item.isActive(Instant.now()));
    }

    /**
     * Checks whether the given diagnostic item is currently being collected.
     * Automatically stops the session if it has expired.
     *
     * @param item the diagnostic item to check
     * @return true if the item is being actively collected
     */
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

    /**
     * Returns the slow request threshold from the current session's config.
     *
     * @return threshold in milliseconds, or 0 if no session is active
     */
    public long slowRequestThresholdMs() {
        return currentSession().map(item -> item.config().slowRequestThresholdMs()).orElse(0L);
    }

    /**
     * Returns the slow SQL threshold from the current session's config.
     *
     * @return threshold in milliseconds, or 0 if no session is active
     */
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
