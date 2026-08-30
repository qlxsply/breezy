package com.corwin.system.diagnostic.infrastructure.db;

import com.corwin.system.diagnostic.infrastructure.collector.SqlExecutionObserver;
import com.corwin.system.diagnostic.infrastructure.runtime.DiagnosticRuntimeManager;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * A DataSource decorator that wraps returned Connections with DiagnosticConnectionProxy to enable
 * SQL execution monitoring.
 *
 * @author Corwin 2026/4/16
 */
public class DiagnosticMonitoringDataSource extends DelegatingDataSource {

  private final String beanName;
  private final DiagnosticRuntimeManager runtimeManager;
  private final SqlExecutionObserver sqlExecutionObserver;

  public DiagnosticMonitoringDataSource(
      String beanName,
      DataSource targetDataSource,
      DiagnosticRuntimeManager runtimeManager,
      SqlExecutionObserver sqlExecutionObserver) {
    super(targetDataSource);
    this.beanName = beanName;
    this.runtimeManager = runtimeManager;
    this.sqlExecutionObserver = sqlExecutionObserver;
  }

  /** Returns a proxied connection for SQL monitoring. */
  @Override
  public Connection getConnection() throws SQLException {
    return DiagnosticConnectionProxy.wrap(
        beanName, super.getConnection(), runtimeManager, sqlExecutionObserver);
  }

  /** Returns a proxied connection for SQL monitoring using the given credentials. */
  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return DiagnosticConnectionProxy.wrap(
        beanName, super.getConnection(username, password), runtimeManager, sqlExecutionObserver);
  }
}
