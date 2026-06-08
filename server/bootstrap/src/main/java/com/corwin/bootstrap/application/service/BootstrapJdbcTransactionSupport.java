package com.corwin.bootstrap.application.service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Corwin 2026/5/5
 */
public final class BootstrapJdbcTransactionSupport {

    private BootstrapJdbcTransactionSupport() {
    }

    public static <T> T execute(DataSource dataSource, SqlCallback<T> callback) {
        return execute(dataSource::getConnection, callback);
    }

    public static void executeWithoutResult(DataSource dataSource, SqlRunnable runnable) {
        execute(dataSource, connection -> {
            runnable.run(connection);
            return null;
        });
    }

    public static <T> T execute(ConnectionSupplier connectionSupplier, SqlCallback<T> callback) {
        try (Connection connection = connectionSupplier.get()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = callback.execute(connection);
                connection.commit();
                return result;
            } catch (Exception e) {
                rollback(connection, e);
                throw propagate("Bootstrap transaction execution failed", e);
            } finally {
                restoreAutoCommit(connection, originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Open bootstrap transaction failed", e);
        }
    }

    public static <T> T executeNonTransactional(ConnectionSupplier connectionSupplier, SqlCallback<T> callback) {
        try (Connection connection = connectionSupplier.get()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            if (!originalAutoCommit) {
                connection.setAutoCommit(true);
            }
            try {
                return callback.execute(connection);
            } finally {
                if (!originalAutoCommit) {
                    connection.setAutoCommit(false);
                }
            }
        } catch (Exception e) {
            throw propagate("Bootstrap non-transactional execution failed", e);
        }
    }

    private static void rollback(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
        }
    }

    private static void restoreAutoCommit(Connection connection, boolean originalAutoCommit) {
        try {
            connection.setAutoCommit(originalAutoCommit);
        } catch (SQLException ignored) {
        }
    }

    private static IllegalStateException propagate(String message, Exception e) {
        if (e instanceof IllegalStateException illegalStateException) {
            return illegalStateException;
        }
        return new IllegalStateException(message, e);
    }

    @FunctionalInterface
    public interface ConnectionSupplier {
        Connection get() throws SQLException;
    }

    @FunctionalInterface
    public interface SqlCallback<T> {
        T execute(Connection connection) throws Exception;
    }

    @FunctionalInterface
    public interface SqlRunnable {
        void run(Connection connection) throws Exception;
    }
}
