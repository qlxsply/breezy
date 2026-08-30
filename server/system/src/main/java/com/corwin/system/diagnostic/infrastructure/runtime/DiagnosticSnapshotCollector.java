package com.corwin.system.diagnostic.infrastructure.runtime;

import com.corwin.system.diagnostic.domain.model.DiagnosticConfig;
import com.corwin.system.diagnostic.domain.model.DiagnosticItem;
import com.corwin.system.diagnostic.domain.model.DiagnosticSnapshot;
import com.corwin.system.diagnostic.infrastructure.collector.DbPoolStateCollector;
import com.corwin.system.diagnostic.infrastructure.collector.HttpRequestObserver;
import com.corwin.system.diagnostic.infrastructure.collector.JfrCollector;
import com.corwin.system.diagnostic.infrastructure.collector.JvmStateCollector;
import com.corwin.system.diagnostic.infrastructure.collector.OsStateCollector;
import com.corwin.system.diagnostic.infrastructure.collector.SqlExecutionObserver;
import com.corwin.system.diagnostic.infrastructure.collector.ThreadStateCollector;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Facade that orchestrates all individual collectors to produce a single composite
 * DiagnosticSnapshot.
 *
 * @author Corwin 2026/4/16
 */
@Component
public class DiagnosticSnapshotCollector {

  private final JvmStateCollector jvmStateCollector;
  private final OsStateCollector osStateCollector;
  private final ThreadStateCollector threadStateCollector;
  private final HttpRequestObserver httpRequestObserver;
  private final DbPoolStateCollector dbPoolStateCollector;
  private final SqlExecutionObserver sqlExecutionObserver;
  private final JfrCollector jfrCollector;

  public DiagnosticSnapshotCollector(
      JvmStateCollector jvmStateCollector,
      OsStateCollector osStateCollector,
      ThreadStateCollector threadStateCollector,
      HttpRequestObserver httpRequestObserver,
      DbPoolStateCollector dbPoolStateCollector,
      SqlExecutionObserver sqlExecutionObserver,
      JfrCollector jfrCollector) {
    this.jvmStateCollector = jvmStateCollector;
    this.osStateCollector = osStateCollector;
    this.threadStateCollector = threadStateCollector;
    this.httpRequestObserver = httpRequestObserver;
    this.dbPoolStateCollector = dbPoolStateCollector;
    this.sqlExecutionObserver = sqlExecutionObserver;
    this.jfrCollector = jfrCollector;
  }

  /**
   * Collects a full snapshot by invoking each collector based on the enabled diagnostic items.
   *
   * @param config the diagnostic config determining which items to collect
   * @return a composite DiagnosticSnapshot
   */
  public DiagnosticSnapshot collect(DiagnosticConfig config) {
    return new DiagnosticSnapshot(
        Instant.now(),
        config.includes(DiagnosticItem.JVM) ? jvmStateCollector.collect() : null,
        config.includes(DiagnosticItem.OS) ? osStateCollector.collect() : null,
        config.includes(DiagnosticItem.THREAD) ? threadStateCollector.collect() : null,
        config.includes(DiagnosticItem.HTTP) ? httpRequestObserver.snapshot() : null,
        config.includes(DiagnosticItem.DB_POOL) ? dbPoolStateCollector.snapshot() : null,
        config.includes(DiagnosticItem.SQL) ? sqlExecutionObserver.snapshot() : null,
        config.includes(DiagnosticItem.JFR) ? jfrCollector.snapshot() : null);
  }
}
