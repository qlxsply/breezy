package com.corwin.system.diagnostic.infrastructure.collector;

import com.corwin.system.diagnostic.domain.model.DiagnosticEvent;
import com.corwin.system.diagnostic.domain.model.DiagnosticEventType;
import com.corwin.system.diagnostic.domain.model.SqlSnapshot;
import com.corwin.system.diagnostic.domain.model.SqlStatementStatSnapshot;
import com.corwin.system.diagnostic.domain.repo.DiagnosticEventRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Corwin 2026/4/16
 */
@Component
public class SqlExecutionObserver {

    private static final int MAX_SQL_STATS = 512;
    private static final int MAX_SQL_LENGTH = 512;

    private final DiagnosticEventRepository eventRepository;
    private final LongAdder totalExecutions = new LongAdder();
    private final LongAdder totalDurationMs = new LongAdder();
    private final LongAdder slowSqlCount = new LongAdder();
    private final LongAdder errorCount = new LongAdder();
    private final ConcurrentHashMap<String, SqlAggregate> sqlStats = new ConcurrentHashMap<>();

    public SqlExecutionObserver(DiagnosticEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void record(String dataSourceName, String sql, long durationMs, Throwable error, long slowSqlThresholdMs) {
        String normalizedDataSourceName = normalizeDataSourceName(dataSourceName);
        String normalizedSql = normalizeSql(sql);
        totalExecutions.increment();
        totalDurationMs.add(Math.max(0L, durationMs));

        SqlAggregate aggregate = sqlStats.computeIfAbsent(normalizedDataSourceName + "|" + normalizedSql,
                _key -> new SqlAggregate(normalizedDataSourceName, normalizedSql));
        aggregate.totalExecutions.increment();
        aggregate.totalDurationMs.add(Math.max(0L, durationMs));
        aggregate.maxDurationMs.accumulate(Math.max(0L, durationMs));

        if (durationMs >= slowSqlThresholdMs) {
            slowSqlCount.increment();
            aggregate.slowExecutions.increment();
            eventRepository.append(new DiagnosticEvent(UUID.randomUUID().toString(), DiagnosticEventType.SQL_SLOW,
                    Instant.now(), "慢 SQL", normalizedSql,
                    Map.of("dataSourceName", normalizedDataSourceName, "durationMs", durationMs)));
        }
        if (error != null) {
            errorCount.increment();
            aggregate.errorExecutions.increment();
            eventRepository.append(new DiagnosticEvent(UUID.randomUUID().toString(), DiagnosticEventType.SQL_ERROR,
                    Instant.now(), "SQL 异常", normalizedSql,
                    Map.of("dataSourceName", normalizedDataSourceName, "durationMs", durationMs, "error",
                            error.getClass().getSimpleName())));
        }

        trimSqlStatsIfNecessary();
    }

    public SqlSnapshot snapshot() {
        long total = totalExecutions.sum();
        long duration = totalDurationMs.sum();
        return new SqlSnapshot(total, total <= 0L ? 0D : (double) duration / total, slowSqlCount.sum(),
                errorCount.sum(), sqlStats.values().stream()
                        .sorted(Comparator.comparingLong(SqlAggregate::totalExecutionsValue).reversed()
                                .thenComparing(SqlAggregate::sql))
                        .limit(20)
                        .map(SqlAggregate::toSnapshot)
                        .toList());
    }

    public void reset() {
        totalExecutions.reset();
        totalDurationMs.reset();
        slowSqlCount.reset();
        errorCount.reset();
        sqlStats.clear();
    }

    private void trimSqlStatsIfNecessary() {
        if (sqlStats.size() <= MAX_SQL_STATS) {
            return;
        }
        sqlStats.values().stream()
                .sorted(Comparator.comparingLong(SqlAggregate::totalExecutionsValue))
                .limit(Math.max(1, sqlStats.size() - MAX_SQL_STATS))
                .map(SqlAggregate::key)
                .toList()
                .forEach(sqlStats::remove);
    }

    private String normalizeDataSourceName(String dataSourceName) {
        return dataSourceName == null || dataSourceName.isBlank() ? "default" : dataSourceName.trim();
    }

    private String normalizeSql(String sql) {
        if (sql == null || sql.isBlank()) {
            return "<unknown>";
        }
        String normalized = sql.replaceAll("\\s+", " ").trim();
        if (normalized.length() > MAX_SQL_LENGTH) {
            return normalized.substring(0, MAX_SQL_LENGTH);
        }
        return normalized;
    }

    private static final class SqlAggregate {

        private final String dataSourceName;
        private final String sql;
        private final LongAdder totalExecutions = new LongAdder();
        private final LongAdder totalDurationMs = new LongAdder();
        private final LongAdder slowExecutions = new LongAdder();
        private final LongAdder errorExecutions = new LongAdder();
        private final java.util.concurrent.atomic.LongAccumulator maxDurationMs =
                new java.util.concurrent.atomic.LongAccumulator(Long::max, 0L);

        private SqlAggregate(String dataSourceName, String sql) {
            this.dataSourceName = dataSourceName;
            this.sql = sql;
        }

        private String key() {
            return dataSourceName + "|" + sql;
        }

        private String sql() {
            return sql;
        }

        private long totalExecutionsValue() {
            return totalExecutions.sum();
        }

        private SqlStatementStatSnapshot toSnapshot() {
            long executions = totalExecutions.sum();
            long duration = totalDurationMs.sum();
            return new SqlStatementStatSnapshot(dataSourceName, sql, executions, duration,
                    executions <= 0L ? 0L : duration / executions, slowExecutions.sum(), errorExecutions.sum(),
                    maxDurationMs.get());
        }
    }
}
