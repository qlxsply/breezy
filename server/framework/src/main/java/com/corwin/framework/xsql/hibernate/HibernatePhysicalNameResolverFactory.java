package com.corwin.framework.xsql.hibernate;

import com.corwin.framework.xsql.PhysicalNameResolver;
import com.corwin.framework.xsql.error.XSqlNamingStrategyException;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Hibernate 物理命名解析器工厂。
 * <p>
 * 负责从 JPA 运行时提取 Hibernate 实际生效的 PhysicalNamingStrategy，
 * 并构造与其对齐的 XSql 命名解析器。
 *
 * @author Corwin 2026/4/17
 */
public final class HibernatePhysicalNameResolverFactory {

    private HibernatePhysicalNameResolverFactory() {
    }

    /**
     * 从 EntityManagerFactory 构造 Hibernate 物理命名解析器。
     */
    public static PhysicalNameResolver create(EntityManagerFactory entityManagerFactory) {
        if (entityManagerFactory == null) {
            throw new XSqlNamingStrategyException("EntityManagerFactory is required for XSql naming resolution");
        }
        SessionFactoryImplementor sessionFactory;
        try {
            sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        } catch (RuntimeException ex) {
            throw new XSqlNamingStrategyException(
                    "XSql requires Hibernate as JPA provider to resolve physical naming strategy", ex);
        }

        PhysicalNamingStrategy physicalNamingStrategy = resolvePhysicalNamingStrategy(entityManagerFactory,
                sessionFactory);

        JdbcEnvironment jdbcEnvironment = sessionFactory.getJdbcServices().getJdbcEnvironment();
        if (jdbcEnvironment == null) {
            throw new XSqlNamingStrategyException("Hibernate JdbcEnvironment is not available");
        }

        return new HibernatePhysicalNameResolver(physicalNamingStrategy, jdbcEnvironment);
    }

    private static PhysicalNamingStrategy resolvePhysicalNamingStrategy(EntityManagerFactory entityManagerFactory,
            SessionFactoryImplementor sessionFactory) {
        Object configured = readSetting(sessionFactory.getProperties());
        if (configured == null) {
            configured = readSetting(entityManagerFactory.getProperties());
        }
        if (configured == null) {
            return new CamelCaseToUnderscoresNamingStrategy();
        }
        if (configured instanceof PhysicalNamingStrategy strategy) {
            return strategy;
        }
        if (configured instanceof Class<?> type) {
            return instantiateStrategy(type);
        }
        if (configured instanceof String className) {
            return instantiateStrategy(className);
        }
        throw new XSqlNamingStrategyException(
                "Unsupported Hibernate physical naming strategy config type: " + configured.getClass().getName());
    }

    private static Object readSetting(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return null;
        }
        return properties.get(MappingSettings.PHYSICAL_NAMING_STRATEGY);
    }

    private static PhysicalNamingStrategy instantiateStrategy(String className) {
        if (className == null || className.isBlank()) {
            throw new XSqlNamingStrategyException("Hibernate physical naming strategy class name is blank");
        }
        try {
            Class<?> type = Class.forName(className.trim());
            return instantiateStrategy(type);
        } catch (ClassNotFoundException ex) {
            throw new XSqlNamingStrategyException(
                    "Hibernate physical naming strategy class not found: " + className, ex);
        }
    }

    private static PhysicalNamingStrategy instantiateStrategy(Class<?> type) {
        if (!PhysicalNamingStrategy.class.isAssignableFrom(type)) {
            throw new XSqlNamingStrategyException(
                    "Configured class does not implement PhysicalNamingStrategy: " + type.getName());
        }
        try {
            return (PhysicalNamingStrategy) type.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                InvocationTargetException ex) {
            throw new XSqlNamingStrategyException(
                    "Instantiate Hibernate physical naming strategy failed: " + type.getName(), ex);
        }
    }
}
