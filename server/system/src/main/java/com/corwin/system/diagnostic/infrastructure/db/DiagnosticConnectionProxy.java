package com.corwin.system.diagnostic.infrastructure.db;

import com.corwin.system.diagnostic.infrastructure.collector.SqlExecutionObserver;
import com.corwin.system.diagnostic.infrastructure.runtime.DiagnosticRuntimeManager;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * JDBC proxy that wraps Connection instances to intercept statement creation and wrap each created
 * Statement with DiagnosticStatementProxy for SQL monitoring.
 *
 * @author Corwin 2026/4/16
 */
public final class DiagnosticConnectionProxy implements InvocationHandler {

  private final String dataSourceName;
  private final Connection target;
  private final DiagnosticRuntimeManager runtimeManager;
  private final SqlExecutionObserver sqlExecutionObserver;

  private DiagnosticConnectionProxy(
      String dataSourceName,
      Connection target,
      DiagnosticRuntimeManager runtimeManager,
      SqlExecutionObserver sqlExecutionObserver) {
    this.dataSourceName = dataSourceName;
    this.target = target;
    this.runtimeManager = runtimeManager;
    this.sqlExecutionObserver = sqlExecutionObserver;
  }

  /**
   * Wraps a Connection with a diagnostic proxy.
   *
   * @param dataSourceName the data source name
   * @param target the original connection to wrap
   * @param runtimeManager the runtime manager for checking collection state
   * @param sqlExecutionObserver the SQL execution observer
   * @return a proxied Connection, or null if the target was null
   */
  public static Connection wrap(
      String dataSourceName,
      Connection target,
      DiagnosticRuntimeManager runtimeManager,
      SqlExecutionObserver sqlExecutionObserver) {
    if (target == null) {
      return null;
    }
    return (Connection)
        Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class[] {Connection.class},
            new DiagnosticConnectionProxy(
                dataSourceName, target, runtimeManager, sqlExecutionObserver));
  }

  /**
   * Intercepts connection method calls and wraps any created statements with diagnostic proxies.
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    if ("unwrap".equals(methodName)
        && args != null
        && args.length == 1
        && args[0] instanceof Class<?> clazz) {
      if (clazz.isInstance(target)) {
        return target;
      }
    }
    if ("isWrapperFor".equals(methodName)
        && args != null
        && args.length == 1
        && args[0] instanceof Class<?> clazz) {
      if (clazz.isInstance(target)) {
        return true;
      }
    }
    try {
      Object result = method.invoke(target, args);
      return switch (methodName) {
        case "createStatement" ->
            DiagnosticStatementProxy.wrap(
                dataSourceName, (Statement) result, null, runtimeManager, sqlExecutionObserver);
        case "prepareStatement" ->
            DiagnosticStatementProxy.wrap(
                dataSourceName,
                (PreparedStatement) result,
                args != null && args.length > 0 && args[0] instanceof String sql ? sql : null,
                runtimeManager,
                sqlExecutionObserver);
        case "prepareCall" ->
            DiagnosticStatementProxy.wrap(
                dataSourceName,
                (CallableStatement) result,
                args != null && args.length > 0 && args[0] instanceof String sql ? sql : null,
                runtimeManager,
                sqlExecutionObserver);
        default -> result;
      };
    } catch (InvocationTargetException ex) {
      throw ex.getTargetException();
    }
  }
}
