package com.corwin.system.diagnostic.infrastructure.db;

import com.corwin.system.diagnostic.domain.model.DiagnosticItem;
import com.corwin.system.diagnostic.infrastructure.collector.SqlExecutionObserver;
import com.corwin.system.diagnostic.infrastructure.runtime.DiagnosticRuntimeManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC proxy that intercepts Statement method calls to record SQL execution metrics
 * via SqlExecutionObserver. Supports prepared, callable, and batch statements.
 *
 * @author Corwin 2026/4/16
 */
public final class DiagnosticStatementProxy implements InvocationHandler {

    private final String dataSourceName;
    private final Statement target;
    private final String preparedSql;
    private final DiagnosticRuntimeManager runtimeManager;
    private final SqlExecutionObserver sqlExecutionObserver;
    private final List<String> batchSqlList = new ArrayList<>();

    private DiagnosticStatementProxy(String dataSourceName, Statement target, String preparedSql,
                                     DiagnosticRuntimeManager runtimeManager,
                                     SqlExecutionObserver sqlExecutionObserver) {
        this.dataSourceName = dataSourceName;
        this.target = target;
        this.preparedSql = preparedSql;
        this.runtimeManager = runtimeManager;
        this.sqlExecutionObserver = sqlExecutionObserver;
    }

    /**
     * Wraps a Statement (or PreparedStatement/CallableStatement) with a diagnostic proxy.
     *
     * @param dataSourceName      the data source name
     * @param statement           the original statement to wrap
     * @param preparedSql         the prepared SQL for PreparedStatement, or null
     * @param runtimeManager      the runtime manager for checking collection state
     * @param sqlExecutionObserver the SQL execution observer
     * @return a proxied Statement, or null if the input statement was null
     */
    public static Statement wrap(String dataSourceName, Statement statement, String preparedSql,
                                 DiagnosticRuntimeManager runtimeManager,
                                 SqlExecutionObserver sqlExecutionObserver) {
        if (statement == null) {
            return null;
        }
        Class<?> interfaceType = statement instanceof CallableStatement ? CallableStatement.class :
                statement instanceof PreparedStatement ? PreparedStatement.class : Statement.class;
        return (Statement) Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class[]{interfaceType},
                new DiagnosticStatementProxy(dataSourceName, statement, preparedSql, runtimeManager, sqlExecutionObserver));
    }

    /**
     * Intercepts execute methods on the statement to record timing and errors.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if ("addBatch".equals(methodName) && args != null && args.length > 0 && args[0] instanceof String sql) {
            batchSqlList.add(sql);
        } else if ("clearBatch".equals(methodName)) {
            batchSqlList.clear();
        } else if ("unwrap".equals(methodName) && args != null && args.length == 1 && args[0] instanceof Class<?> clazz) {
            if (clazz.isInstance(target)) {
                return target;
            }
        } else if ("isWrapperFor".equals(methodName) && args != null && args.length == 1 && args[0] instanceof Class<?> clazz) {
            if (clazz.isInstance(target)) {
                return true;
            }
        }

        if (!isExecuteMethod(methodName) || !runtimeManager.isCollecting(DiagnosticItem.SQL)) {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }

        String sql = resolveSql(args);
        long start = System.currentTimeMillis();
        try {
            Object result = method.invoke(target, args);
            sqlExecutionObserver.record(dataSourceName, sql, System.currentTimeMillis() - start, null,
                    runtimeManager.slowSqlThresholdMs());
            if ("executeBatch".equals(methodName) || "executeLargeBatch".equals(methodName)) {
                batchSqlList.clear();
            }
            return result;
        } catch (InvocationTargetException ex) {
            Throwable targetException = ex.getTargetException();
            sqlExecutionObserver.record(dataSourceName, sql, System.currentTimeMillis() - start, targetException,
                    runtimeManager.slowSqlThresholdMs());
            throw targetException;
        }
    }

    private boolean isExecuteMethod(String methodName) {
        return "execute".equals(methodName) || "executeQuery".equals(methodName) || "executeUpdate".equals(methodName)
                || "executeLargeUpdate".equals(methodName) || "executeBatch".equals(methodName)
                || "executeLargeBatch".equals(methodName);
    }

    private String resolveSql(Object[] args) {
        if (args != null && args.length > 0 && args[0] instanceof String sql) {
            return sql;
        }
        if (!batchSqlList.isEmpty()) {
            return String.join(" ; ", batchSqlList);
        }
        return preparedSql;
    }
}
