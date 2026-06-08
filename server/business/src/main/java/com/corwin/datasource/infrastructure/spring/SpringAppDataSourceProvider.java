package com.corwin.datasource.infrastructure.spring;

import com.corwin.datasource.application.port.AppDataSourceProvider;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Wrapper;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Corwin 2026/2/7
 */
@Component
public class SpringAppDataSourceProvider implements AppDataSourceProvider {

    private final Map<String, DataSource> dataSources;

    public SpringAppDataSourceProvider(ListableBeanFactory beanFactory) {
        this.dataSources = new LinkedHashMap<>(beanFactory.getBeansOfType(DataSource.class));
    }

    @Override
    public Map<String, DataSource> listAll() {
        return Collections.unmodifiableMap(dataSources);
    }

    @Override
    public Optional<DataSource> findByKey(String key) {
        return Optional.ofNullable(dataSources.get(key));
    }

    @Override
    public Optional<String> findPasswordByKey(String key) {
        DataSource raw = dataSources.get(key);
        if (raw == null) {
            return Optional.empty();
        }
        DataSource target = unwrapWithChain(raw);
        if (target == null) {
            return Optional.empty();
        }
        if (target instanceof HikariDataSource hikari) {
            return Optional.ofNullable(hikari.getPassword());
        }
        return readStringProperty(target, "getPassword")
                .or(() -> readStringProperty(target, "getPasswd"));
    }

    private DataSource unwrapWithChain(DataSource dataSource) {
        Set<DataSource> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        DataSource current = dataSource;
        int depth = 0;
        while (current != null && depth < 16 && visited.add(current)) {
            DataSource next = unwrapOnce(current);
            if (next == null || next == current) {
                return current;
            }
            current = next;
            depth++;
        }
        return current;
    }

    private DataSource unwrapOnce(DataSource dataSource) {
        if (dataSource instanceof DelegatingDataSource delegating) {
            DataSource target = delegating.getTargetDataSource();
            if (target != null) {
                return target;
            }
        }
        if (dataSource instanceof Wrapper wrapper) {
            try {
                if (wrapper.isWrapperFor(HikariDataSource.class)) {
                    return wrapper.unwrap(HikariDataSource.class);
                }
                if (wrapper.isWrapperFor(DataSource.class)) {
                    DataSource unwrapped = wrapper.unwrap(DataSource.class);
                    if (unwrapped != null && unwrapped != dataSource) {
                        return unwrapped;
                    }
                }
            } catch (Exception ex) {
                return dataSource;
            }
        }
        for (String methodName : List.of("getTargetDataSource", "getDelegate", "getDataSource", "getRealDataSource",
                "getWrappedDataSource")) {
            Optional<DataSource> unwrapped = readDataSourceProperty(dataSource, methodName);
            if (unwrapped.isPresent() && unwrapped.get() != dataSource) {
                return unwrapped.get();
            }
        }
        return dataSource;
    }

    private Optional<String> readStringProperty(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            if (method.getReturnType() != String.class) {
                return Optional.empty();
            }
            return Optional.ofNullable((String) method.invoke(target));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Optional<DataSource> readDataSourceProperty(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            if (!DataSource.class.isAssignableFrom(method.getReturnType())) {
                return Optional.empty();
            }
            return Optional.ofNullable((DataSource) method.invoke(target));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
