package com.corwin.datasource.infrastructure.jdbc;

import com.corwin.datasource.application.port.AppDataSourceProvider;
import com.corwin.datasource.application.port.SecretCodec;
import com.corwin.datasource.domain.model.DatabaseSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author Corwin 2026/1/11
 */
@Component
@RequiredArgsConstructor
public class JdbcConnectionFactory {

  private final SecretCodec secretCodec;
  private final AppDataSourceProvider appDataSourceProvider;

  public Connection openConnection(DatabaseSource source) throws SQLException {
    javax.sql.DataSource dataSource = buildDataSource(source);
    return dataSource.getConnection();
  }

  /** 每次构造一个临时 javax.sql.DataSource（简单直接）。 若连接会频繁使用，可做 dataSourceId 维度缓存（注意密码变更、连接泄漏、上限等）。 */
  public javax.sql.DataSource buildDataSource(DatabaseSource source) {
    if (source.isAppSource()) {
      String key = source.getAppDsKey();
      if (key == null || key.isBlank()) {
        throw new IllegalArgumentException("App DataSource key is missing");
      }
      return appDataSourceProvider
          .findByKey(key)
          .orElseThrow(() -> new IllegalArgumentException("App DataSource not found: " + key));
    }

    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl(source.getJdbcUrl());
    cfg.setUsername(source.getUsername());
    String passwordEnc = source.getPasswordEnc();
    if (passwordEnc != null) {
      cfg.setPassword(secretCodec.decode(passwordEnc));
    }

    // 通用连接参数
    cfg.setMaximumPoolSize(2);
    cfg.setMinimumIdle(0);
    cfg.setConnectionTimeout(Duration.ofSeconds(8).toMillis());
    cfg.setValidationTimeout(Duration.ofSeconds(5).toMillis());

    // 驱动类一般可省略（JDBC 4 自动发现），但某些环境显式指定更稳
    DatabaseMetaSupport.applyDriverClassIfNeeded(cfg, source);

    return new HikariDataSource(cfg);
  }
}
