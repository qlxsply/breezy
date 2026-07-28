package com.corwin.framework.mybatis.pagination;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MyBatis PageData 分页配置。
 *
 * @author Corwin 2026/7/28
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "corwin.mybatis.pagination")
public class MybatisPaginationProperties {

    private boolean enabled = true;

    /**
     * 最大分页大小。
     * 超出时抛出 PaginationException，不进行静默截断。
     */
    private int maxPageSize = 500;

    /**
     * 动态 SQL 可能产生不同的最终 SQL。
     * 这里缓存解析后的 count SQL，采用有界 LRU。
     */
    private int countSqlCacheSize = 1024;

}
