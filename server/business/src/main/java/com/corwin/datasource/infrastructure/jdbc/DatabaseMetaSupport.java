package com.corwin.datasource.infrastructure.jdbc;

import com.corwin.datasource.domain.model.DatabaseSource;
import com.corwin.datasource.domain.model.DatabaseType;
import com.corwin.framework.util.StrUtil;
import com.zaxxer.hikari.HikariConfig;

import java.util.Properties;


/**
 * 处理跨库差异：驱动类、remarks 支持等。
 * 注意：是否支持 remarks 取决于驱动与连接参数，且不是所有数据库/驱动都能通过 DatabaseMetaData 拿到备注。
 *
 * @author Corwin 2026/1/11
 */
public final class DatabaseMetaSupport {

    private DatabaseMetaSupport() {
    }

    public static void applyDriverClassIfNeeded(HikariConfig cfg, DatabaseSource source) {
        if (source.getDbType() == null) {
            return;
        }

        if (StrUtil.isNotBlank(source.getDriverClassName())) {
            cfg.setDriverClassName(source.getDriverClassName().trim());
        } else {
            switch (source.getDbType()) {
                case H2 -> cfg.setDriverClassName("org.h2.Driver");
                case MYSQL -> cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
                case POSTGRESQL -> cfg.setDriverClassName("org.postgresql.Driver");
                case ORACLE -> cfg.setDriverClassName("oracle.jdbc.OracleDriver");
                case SQLSERVER -> cfg.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            }
        }

        // 尝试开启 remarks（不保证）
        Properties props = new Properties();

        // MySQL：useInformationSchema=true 有助于 remarks
        if (source.getDbType() == DatabaseType.MYSQL) {
            props.setProperty("useInformationSchema", "true");
            props.setProperty("remarks", "true");
        }

        // Oracle：remarksReporting=true
        if (source.getDbType() == DatabaseType.ORACLE) {
            props.setProperty("remarksReporting", "true");
        }

        // SQLServer：remarks 未必生效，但保留扩展点
        // PG：一般 remarks 能拿到，但也取决于驱动与权限

        cfg.setDataSourceProperties(props);
    }

    /**
     * 选择用于抓取的 schema/catalog（跨库尽量通用）：
     * - 优先使用连接配置 defaultSchema
     * - 否则使用 Connection.getSchema() / getCatalog() 或 meta.getUserName()（Oracle 常用）
     */
    public static String chooseSchema(String configured, String connSchema, String metaUser) {
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        if (connSchema != null && !connSchema.isBlank()) {
            return connSchema.trim();
        }
        if (metaUser != null && !metaUser.isBlank()) {
            return metaUser.trim();
        }
        return null;
    }
}
