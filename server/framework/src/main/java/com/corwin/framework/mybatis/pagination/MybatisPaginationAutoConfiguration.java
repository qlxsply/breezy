package com.corwin.framework.mybatis.pagination;

import com.corwin.framework.mybatis.pagination.count.JSqlParserCountSqlOptimizer;
import com.corwin.framework.mybatis.pagination.dialect.MySqlPaginationDialect;
import com.corwin.framework.mybatis.pagination.dialect.PaginationDialect;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

/**
 * MyBatis PageData 分页插件自动配置。
 *
 * @author Corwin 2026/7/28
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class,
        beforeName = "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration")
@ConditionalOnClass({SqlSessionFactory.class, Interceptor.class, CCJSqlParserUtil.class})
@ConditionalOnSingleCandidate(DataSource.class)
@ConditionalOnProperty(prefix = "corwin.mybatis.pagination", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(MybatisPaginationProperties.class)
public class MybatisPaginationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PageMethodResolver pageMethodResolver() {
        return new PageMethodResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public PaginationDialect paginationDialect(DataSource dataSource) {
        String productName = resolveDatabaseProductName(dataSource);
        String normalized = productName == null ? "" : productName.toLowerCase(Locale.ROOT);

        if (normalized.contains("mysql") || normalized.contains("mariadb")) {
            return new MySqlPaginationDialect();
        }

        throw new PaginationException("Unsupported pagination database product: " + productName);
    }

    @Bean
    @ConditionalOnMissingBean
    public JSqlParserCountSqlOptimizer countSqlOptimizer(MybatisPaginationProperties properties) {
        return new JSqlParserCountSqlOptimizer(properties.getCountSqlCacheSize());
    }

    @Bean
    @ConditionalOnMissingBean(PageDataPaginationInterceptor.class)
    public PageDataPaginationInterceptor pageDataPaginationInterceptor(PageMethodResolver methodResolver,
            PaginationDialect dialect, JSqlParserCountSqlOptimizer optimizer, MybatisPaginationProperties properties) {
        return new PageDataPaginationInterceptor(methodResolver, dialect, optimizer, properties);
    }

    private String resolveDatabaseProductName(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return metaData.getDatabaseProductName();
        } catch (SQLException exception) {
            throw new PaginationException("Resolve pagination database product failed", exception);
        }
    }
}
