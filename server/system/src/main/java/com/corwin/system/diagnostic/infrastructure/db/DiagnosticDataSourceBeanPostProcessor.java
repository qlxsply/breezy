package com.corwin.system.diagnostic.infrastructure.db;

import com.corwin.system.diagnostic.infrastructure.collector.DbPoolStateCollector;
import com.corwin.system.diagnostic.infrastructure.collector.SqlExecutionObserver;
import com.corwin.system.diagnostic.infrastructure.runtime.DiagnosticRuntimeManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * @author Corwin 2026/4/16
 */
@Component
public class DiagnosticDataSourceBeanPostProcessor implements BeanPostProcessor {

    private final DiagnosticRuntimeManager runtimeManager;
    private final SqlExecutionObserver sqlExecutionObserver;
    private final DbPoolStateCollector dbPoolStateCollector;

    public DiagnosticDataSourceBeanPostProcessor(DiagnosticRuntimeManager runtimeManager,
                                                 SqlExecutionObserver sqlExecutionObserver,
                                                 DbPoolStateCollector dbPoolStateCollector) {
        this.runtimeManager = runtimeManager;
        this.sqlExecutionObserver = sqlExecutionObserver;
        this.dbPoolStateCollector = dbPoolStateCollector;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof DataSource dataSource)) {
            return bean;
        }
        if (bean instanceof DiagnosticMonitoringDataSource) {
            dbPoolStateCollector.register(beanName, dataSource);
            return bean;
        }
        DiagnosticMonitoringDataSource wrapped =
                new DiagnosticMonitoringDataSource(beanName, dataSource, runtimeManager, sqlExecutionObserver);
        dbPoolStateCollector.register(beanName, wrapped);
        return wrapped;
    }
}
