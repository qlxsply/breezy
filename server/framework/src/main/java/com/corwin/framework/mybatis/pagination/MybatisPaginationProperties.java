package com.corwin.framework.mybatis.pagination;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the MyBatis {@link PageData} pagination plugin.
 *
 * @author Corwin 2026/7/28
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "corwin.mybatis.pagination")
public class MybatisPaginationProperties {

  private boolean enabled = true;

  /** Maximum allowed page size. Exceeding this value triggers a {@link PaginationException}. */
  private int maxPageSize = 500;

  /**
   * LRU cache size for parsed count-SQL results. Dynamic SQL may generate different final SQL
   * strings, so caching avoids repeated parsing.
   */
  private int countSqlCacheSize = 1024;
}
