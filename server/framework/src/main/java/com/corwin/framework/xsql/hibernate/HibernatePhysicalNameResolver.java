package com.corwin.framework.xsql.hibernate;

import com.corwin.framework.xsql.PhysicalNameResolver;
import com.corwin.framework.xsql.error.XSqlNamingStrategyException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * 基于 Hibernate PhysicalNamingStrategy 的物理命名解析器。
 * <p>
 * 该实现复用 Hibernate 运行时已生效的 physical naming strategy，
 * 确保 XSql 对表名与列名的解析结果与 JPA 最终 SQL 保持一致。
 *
 * @author Corwin 2026/4/17
 */
public class HibernatePhysicalNameResolver implements PhysicalNameResolver {

    private final PhysicalNamingStrategy physicalNamingStrategy;
    private final JdbcEnvironment jdbcEnvironment;

    public HibernatePhysicalNameResolver(PhysicalNamingStrategy physicalNamingStrategy,
            JdbcEnvironment jdbcEnvironment) {
        this.physicalNamingStrategy = physicalNamingStrategy;
        this.jdbcEnvironment = jdbcEnvironment;
    }

    @Override
    public String resolveTableName(Class<?> entityClass, String logicalTableName) {
        String logicalName = logicalTableName;
        if (logicalName == null || logicalName.isBlank()) {
            logicalName = entityClass == null ? null : entityClass.getSimpleName();
        }
        return resolveIdentifier(entityClass == null ? "unknown-entity" : entityClass.getName(), logicalName, true);
    }

    @Override
    public String resolveColumnName(Class<?> ownerType, String propertyPath, String logicalColumnName) {
        String logicalName = logicalColumnName;
        if (logicalName == null || logicalName.isBlank()) {
            logicalName = propertyPath;
            int idx = propertyPath == null ? -1 : propertyPath.lastIndexOf('.');
            if (idx >= 0 && idx < propertyPath.length() - 1) {
                logicalName = propertyPath.substring(idx + 1);
            }
        }
        String ownerName = ownerType == null ? "unknown-owner" : ownerType.getName();
        return resolveIdentifier(ownerName + "#" + propertyPath, logicalName, false);
    }

    private String resolveIdentifier(String target, String logicalName, boolean table) {
        if (logicalName == null || logicalName.isBlank()) {
            throw new XSqlNamingStrategyException("Logical name is blank for target: " + target);
        }
        try {
            Identifier logicalIdentifier = Identifier.toIdentifier(logicalName.trim());
            Identifier physicalIdentifier = table
                    ? physicalNamingStrategy.toPhysicalTableName(logicalIdentifier, jdbcEnvironment)
                    : physicalNamingStrategy.toPhysicalColumnName(logicalIdentifier, jdbcEnvironment);
            if (physicalIdentifier == null || physicalIdentifier.getText() == null
                    || physicalIdentifier.getText().isBlank()) {
                throw new XSqlNamingStrategyException("Resolved physical name is blank for target: " + target);
            }
            return physicalIdentifier.getText().trim();
        } catch (XSqlNamingStrategyException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new XSqlNamingStrategyException("Resolve Hibernate physical name failed for target: " + target, ex);
        }
    }
}
