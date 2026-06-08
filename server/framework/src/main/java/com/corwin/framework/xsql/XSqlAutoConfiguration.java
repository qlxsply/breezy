package com.corwin.framework.xsql;

import com.corwin.framework.xsql.hibernate.HibernatePhysicalNameResolverFactory;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XSql 自动配置。
 * <p>
 * 默认注册一个基础 {@link XSql} 实例，可通过显式声明同类型 Bean 覆盖。
 *
 * @author Corwin 2026/4/9
 */
@Configuration
public class XSqlAutoConfiguration {

    /**
     * 注册默认物理命名解析器。
     */
    @Bean
    @ConditionalOnMissingBean
    public PhysicalNameResolver physicalNameResolver(EntityManagerFactory entityManagerFactory) {
        return HibernatePhysicalNameResolverFactory.create(entityManagerFactory);
    }

    /**
     * 注册默认 XSql Bean。
     */
    @Bean
    @ConditionalOnMissingBean
    public XSql xSql(PhysicalNameResolver physicalNameResolver) {
        return XSql.builder().physicalNameResolver(physicalNameResolver).build();
    }
}

