package com.corwin.system.diagnostic.infrastructure.collector;

import com.corwin.system.diagnostic.domain.model.DiagnosticConfig;
import com.corwin.system.diagnostic.domain.model.DiagnosticEvent;
import com.corwin.system.diagnostic.domain.model.DiagnosticEventType;
import com.corwin.system.diagnostic.domain.model.JfrSnapshot;
import com.corwin.system.diagnostic.domain.repo.DiagnosticEventRepository;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Corwin 2026/4/16
 */
@Slf4j
@Component
public class JfrCollector {

    private final DiagnosticEventRepository eventRepository;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final LongAdder gcEventCount = new LongAdder();
    private final LongAdder gcPauseTimeMs = new LongAdder();
    private final LongAdder exceptionEventCount = new LongAdder();
    private final LongAdder threadParkEventCount = new LongAdder();
    private final LongAdder monitorBlockedEventCount = new LongAdder();

    private volatile RecordingStream recordingStream;
    private volatile Thread workerThread;

    public JfrCollector(DiagnosticEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public synchronized void start(DiagnosticConfig config) {
        stop();
        reset();
        RecordingStream stream = new RecordingStream();
        stream.enable("jdk.GarbageCollection").withThreshold(Duration.ofMillis(1));
        stream.onEvent("jdk.GarbageCollection", this::handleGcEvent);
        if (config.deepMode()) {
            stream.enable("jdk.JavaExceptionThrow").withThreshold(Duration.ZERO);
            stream.onEvent("jdk.JavaExceptionThrow", this::handleExceptionEvent);
            stream.enable("jdk.ThreadPark").withThreshold(Duration.ofMillis(1));
            stream.onEvent("jdk.ThreadPark", this::handleThreadParkEvent);
            stream.enable("jdk.JavaMonitorEnter").withThreshold(Duration.ofMillis(1));
            stream.onEvent("jdk.JavaMonitorEnter", this::handleMonitorBlockedEvent);
        }
        recordingStream = stream;
        running.set(true);
        workerThread = Thread.ofPlatform().daemon(true).name("diagnostic-jfr").start(() -> {
            try (RecordingStream ignored = stream) {
                stream.start();
            } catch (Throwable ex) {
                log.warn("Start diagnostic JFR stream failed", ex);
            } finally {
                running.set(false);
            }
        });
    }

    public synchronized void stop() {
        RecordingStream current = recordingStream;
        recordingStream = null;
        if (current != null) {
            try {
                current.close();
            } catch (Exception ignored) {
            }
        }
        running.set(false);
        workerThread = null;
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isAvailable() {
        return true;
    }

    public JfrSnapshot snapshot() {
        return new JfrSnapshot(running.get(), gcEventCount.sum(), gcPauseTimeMs.sum(), exceptionEventCount.sum(),
                threadParkEventCount.sum(), monitorBlockedEventCount.sum());
    }

    public void reset() {
        gcEventCount.reset();
        gcPauseTimeMs.reset();
        exceptionEventCount.reset();
        threadParkEventCount.reset();
        monitorBlockedEventCount.reset();
    }

    private void handleGcEvent(RecordedEvent event) {
        gcEventCount.increment();
        long durationMs = safeDurationMs(event);
        gcPauseTimeMs.add(durationMs);
        eventRepository.append(new DiagnosticEvent(UUID.randomUUID().toString(), DiagnosticEventType.JFR_GC, Instant.now(),
                "GC 事件", event.getEventType().getName(), Map.of("durationMs", durationMs)));
    }

    private void handleExceptionEvent(RecordedEvent event) {
        exceptionEventCount.increment();
        String throwableClass = stringValue(event, "throwableClass");
        eventRepository.append(new DiagnosticEvent(UUID.randomUUID().toString(), DiagnosticEventType.JFR_EXCEPTION,
                Instant.now(), "异常热点", throwableClass, Map.of("message", stringValue(event, "message"))));
    }

    private void handleThreadParkEvent(RecordedEvent event) {
        threadParkEventCount.increment();
        eventRepository.append(new DiagnosticEvent(UUID.randomUUID().toString(), DiagnosticEventType.JFR_THREAD_PARK,
                Instant.now(), "线程等待", event.getEventType().getName(),
                Map.of("durationMs", safeDurationMs(event), "class", stringValue(event, "parkedClass"))));
    }

    private void handleMonitorBlockedEvent(RecordedEvent event) {
        monitorBlockedEventCount.increment();
        eventRepository.append(new DiagnosticEvent(UUID.randomUUID().toString(),
                DiagnosticEventType.JFR_MONITOR_BLOCKED, Instant.now(), "线程阻塞", event.getEventType().getName(),
                Map.of("durationMs", safeDurationMs(event), "monitorClass", stringValue(event, "monitorClass"))));
    }

    private long safeDurationMs(RecordedEvent event) {
        try {
            return event.getDuration().toMillis();
        } catch (Exception ex) {
            return 0L;
        }
    }

    private String stringValue(RecordedEvent event, String fieldName) {
        try {
            Object value = event.getValue(fieldName);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception ex) {
            return "";
        }
    }
}
