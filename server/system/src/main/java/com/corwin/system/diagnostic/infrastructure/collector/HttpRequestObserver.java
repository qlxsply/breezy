package com.corwin.system.diagnostic.infrastructure.collector;

import com.corwin.system.diagnostic.domain.model.DiagnosticEvent;
import com.corwin.system.diagnostic.domain.model.DiagnosticEventType;
import com.corwin.system.diagnostic.domain.model.HttpSnapshot;
import com.corwin.system.diagnostic.domain.model.HttpUriStatSnapshot;
import com.corwin.system.diagnostic.domain.repo.DiagnosticEventRepository;
import com.corwin.system.diagnostic.infrastructure.runtime.DiagnosticRingBuffer;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Corwin 2026/4/16
 */
@Component
public class HttpRequestObserver {

    private static final int DURATION_SAMPLE_CAPACITY = 2048;
    private static final int MAX_URI_STATS = 512;

    private final DiagnosticEventRepository eventRepository;
    private final AtomicInteger inFlightRequests = new AtomicInteger();
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder totalDurationMs = new LongAdder();
    private final LongAdder slowRequestCount = new LongAdder();
    private final LongAdder errorRequestCount = new LongAdder();
    private final ConcurrentHashMap<Integer, LongAdder> statusCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, HttpUriAggregate> uriStats = new ConcurrentHashMap<>();
    private final DiagnosticRingBuffer<Long> durationSamples = new DiagnosticRingBuffer<>(DURATION_SAMPLE_CAPACITY);

    public HttpRequestObserver(DiagnosticEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void onRequestStarted() {
        inFlightRequests.incrementAndGet();
    }

    public void onRequestCompleted(String method, String uri, int status, long durationMs, Throwable error,
                                   long slowThresholdMs) {
        inFlightRequests.updateAndGet(current -> Math.max(0, current - 1));
        totalRequests.increment();
        totalDurationMs.add(Math.max(0L, durationMs));
        durationSamples.add(Math.max(0L, durationMs));
        statusCounts.computeIfAbsent(status, _key -> new LongAdder()).increment();

        String normalizedMethod = normalize(method);
        String normalizedUri = normalizeUri(uri);
        HttpUriAggregate aggregate = uriStats.computeIfAbsent(normalizedMethod + " " + normalizedUri,
                _key -> new HttpUriAggregate(normalizedMethod, normalizedUri));
        aggregate.totalRequests.increment();
        aggregate.totalDurationMs.add(Math.max(0L, durationMs));

        if (durationMs >= slowThresholdMs) {
            slowRequestCount.increment();
            aggregate.slowRequestCount.increment();
            eventRepository.append(new DiagnosticEvent(UUID.randomUUID().toString(), DiagnosticEventType.HTTP_SLOW_REQUEST,
                    Instant.now(), "慢请求", normalizedMethod + " " + normalizedUri,
                    Map.of("status", status, "durationMs", durationMs)));
        }
        if (error != null || status >= 500) {
            errorRequestCount.increment();
            aggregate.errorRequestCount.increment();
            eventRepository.append(new DiagnosticEvent(UUID.randomUUID().toString(), DiagnosticEventType.HTTP_ERROR_REQUEST,
                    Instant.now(), "请求异常", normalizedMethod + " " + normalizedUri,
                    Map.of("status", status, "durationMs", durationMs, "error",
                            error == null ? "" : error.getClass().getSimpleName())));
        }

        trimUriStatsIfNecessary();
    }

    public HttpSnapshot snapshot() {
        long total = totalRequests.sum();
        long totalDuration = totalDurationMs.sum();
        Map<Integer, Long> statusDistribution = new LinkedHashMap<>();
        statusCounts.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                statusDistribution.put(entry.getKey(), entry.getValue().sum()));
        List<HttpUriStatSnapshot> topUris = uriStats.values().stream()
                .sorted(Comparator.comparingLong(HttpUriAggregate::totalRequestsValue).reversed()
                        .thenComparing(HttpUriAggregate::uri))
                .limit(20)
                .map(HttpUriAggregate::toSnapshot)
                .toList();
        List<Long> samples = durationSamples.latest(DURATION_SAMPLE_CAPACITY);
        long p95 = percentile(samples, 95);
        long p99 = percentile(samples, 99);
        return new HttpSnapshot(inFlightRequests.get(), total, total <= 0L ? 0L : totalDuration / total,
                slowRequestCount.sum(), errorRequestCount.sum(), statusDistribution, p95, p99, topUris);
    }

    public void reset() {
        inFlightRequests.set(0);
        totalRequests.reset();
        totalDurationMs.reset();
        slowRequestCount.reset();
        errorRequestCount.reset();
        statusCounts.clear();
        uriStats.clear();
        durationSamples.clear();
    }

    private void trimUriStatsIfNecessary() {
        if (uriStats.size() <= MAX_URI_STATS) {
            return;
        }
        uriStats.values().stream()
                .sorted(Comparator.comparingLong(HttpUriAggregate::totalRequestsValue))
                .limit(Math.max(1, uriStats.size() - MAX_URI_STATS))
                .map(HttpUriAggregate::key)
                .toList()
                .forEach(uriStats::remove);
    }

    private long percentile(List<Long> samples, int percentile) {
        if (samples == null || samples.isEmpty()) {
            return 0L;
        }
        List<Long> sorted = samples.stream().sorted().toList();
        int index = (int) Math.ceil((percentile / 100D) * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value.trim();
    }

    private String normalizeUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return "/";
        }
        String normalized = uri.trim();
        if (normalized.length() > 256) {
            return normalized.substring(0, 256);
        }
        return normalized;
    }

    private static final class HttpUriAggregate {

        private final String method;
        private final String uri;
        private final LongAdder totalRequests = new LongAdder();
        private final LongAdder totalDurationMs = new LongAdder();
        private final LongAdder slowRequestCount = new LongAdder();
        private final LongAdder errorRequestCount = new LongAdder();

        private HttpUriAggregate(String method, String uri) {
            this.method = method;
            this.uri = uri;
        }

        private String key() {
            return method + " " + uri;
        }

        private String uri() {
            return uri;
        }

        private long totalRequestsValue() {
            return totalRequests.sum();
        }

        private HttpUriStatSnapshot toSnapshot() {
            long total = totalRequests.sum();
            long totalDuration = totalDurationMs.sum();
            return new HttpUriStatSnapshot(method, uri, total, totalDuration, total <= 0L ? 0L : totalDuration / total,
                    slowRequestCount.sum(), errorRequestCount.sum());
        }
    }
}
