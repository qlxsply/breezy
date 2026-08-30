package com.corwin.system.diagnostic.infrastructure.collector;

import com.corwin.system.diagnostic.domain.model.DbPoolEntrySnapshot;
import com.corwin.system.diagnostic.domain.model.DbPoolSnapshot;
import java.lang.reflect.Method;
import java.sql.Wrapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.stereotype.Component;

/**
 * Collector that registers and monitors HikariCP connection pools, producing aggregated pool state
 * snapshots.
 *
 * @author Corwin 2026/4/16
 */
@Component
public class DbPoolStateCollector {

  private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

  /**
   * Registers a DataSource bean for monitoring.
   *
   * @param beanName the Spring bean name
   * @param dataSource the DataSource to monitor
   */
  public void register(String beanName, DataSource dataSource) {
    if (beanName == null || beanName.isBlank() || dataSource == null) {
      return;
    }
    dataSources.put(beanName, dataSource);
  }

  /**
   * Returns the sorted list of registered DataSource bean names.
   *
   * @return list of data source names
   */
  public List<String> dataSourceNames() {
    return dataSources.keySet().stream().sorted().toList();
  }

  /**
   * Checks whether at least one DataSource has been registered for monitoring.
   *
   * @return true if any DataSource is registered
   */
  public boolean hasRegisteredDataSource() {
    return !dataSources.isEmpty();
  }

  /**
   * Returns an aggregated snapshot of all registered connection pools.
   *
   * @return a DbPoolSnapshot with combined pool metrics
   */
  public DbPoolSnapshot snapshot() {
    List<DbPoolEntrySnapshot> pools =
        dataSources.entrySet().stream()
            .map(entry -> toSnapshot(entry.getKey(), entry.getValue()))
            .flatMap(Optional::stream)
            .sorted(java.util.Comparator.comparing(DbPoolEntrySnapshot::beanName))
            .toList();
    int active = pools.stream().mapToInt(DbPoolEntrySnapshot::activeConnections).sum();
    int idle = pools.stream().mapToInt(DbPoolEntrySnapshot::idleConnections).sum();
    int total = pools.stream().mapToInt(DbPoolEntrySnapshot::totalConnections).sum();
    int waiting = pools.stream().mapToInt(DbPoolEntrySnapshot::waitingThreads).sum();
    return new DbPoolSnapshot(pools.size(), active, idle, total, waiting, pools);
  }

  private Optional<DbPoolEntrySnapshot> toSnapshot(String beanName, DataSource dataSource) {
    Object hikari = unwrapHikari(dataSource);
    if (hikari == null) {
      return Optional.empty();
    }
    Object poolBean = invokeNoArgs(hikari, "getHikariPoolMXBean");
    if (poolBean == null) {
      return Optional.empty();
    }
    return Optional.of(
        new DbPoolEntrySnapshot(
            beanName,
            stringValue(invokeNoArgs(hikari, "getPoolName")),
            intValue(invokeNoArgs(poolBean, "getActiveConnections")),
            intValue(invokeNoArgs(poolBean, "getIdleConnections")),
            intValue(invokeNoArgs(poolBean, "getTotalConnections")),
            intValue(invokeNoArgs(poolBean, "getThreadsAwaitingConnection"))));
  }

  private Object unwrapHikari(DataSource dataSource) {
    DataSource current = dataSource;
    for (int i = 0; i < 8 && current != null; i++) {
      if (isHikariDataSource(current)) {
        return current;
      }
      if (current instanceof DelegatingDataSource delegatingDataSource) {
        current = delegatingDataSource.getTargetDataSource();
        continue;
      }
      if (current instanceof Wrapper wrapper) {
        try {
          Class<?> hikariClass = hikariDataSourceClass();
          if (hikariClass != null && wrapper.isWrapperFor(hikariClass)) {
            return wrapper.unwrap(hikariClass);
          }
          if (wrapper.isWrapperFor(DataSource.class)) {
            DataSource unwrapped = wrapper.unwrap(DataSource.class);
            if (unwrapped == current) {
              break;
            }
            current = unwrapped;
            continue;
          }
        } catch (Exception ignored) {
        }
      }
      break;
    }
    return null;
  }

  private boolean isHikariDataSource(Object candidate) {
    Class<?> hikariClass = hikariDataSourceClass();
    return hikariClass != null && hikariClass.isInstance(candidate);
  }

  private Class<?> hikariDataSourceClass() {
    try {
      return Class.forName("com.zaxxer.hikari.HikariDataSource");
    } catch (ClassNotFoundException ex) {
      return null;
    }
  }

  private Object invokeNoArgs(Object target, String methodName) {
    if (target == null) {
      return null;
    }
    try {
      Method method = target.getClass().getMethod(methodName);
      return method.invoke(target);
    } catch (Exception ex) {
      return null;
    }
  }

  private int intValue(Object value) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    return 0;
  }

  private String stringValue(Object value) {
    return value == null ? "" : String.valueOf(value);
  }
}
