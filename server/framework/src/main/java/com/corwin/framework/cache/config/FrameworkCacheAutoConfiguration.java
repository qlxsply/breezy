package com.corwin.framework.cache.config;

import com.corwin.framework.cache.CacheTemplate;
import com.corwin.framework.cache.core.CacheKeyValidator;
import com.corwin.framework.cache.core.DefaultCacheTemplate;
import com.corwin.framework.cache.local.CaffeineLocalCacheStore;
import com.corwin.framework.cache.local.LocalCacheProvider;
import com.corwin.framework.cache.local.LocalCacheStore;
import com.corwin.framework.cache.redis.UnsupportedRedisCacheProvider;
import com.corwin.framework.cache.tiered.UnsupportedTieredCacheProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for the cache component.
 *
 * @author Corwin 2026/4/19
 */
@Configuration
@EnableConfigurationProperties(FrameworkCacheProperties.class)
@ConditionalOnProperty(prefix = "framework.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FrameworkCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CacheKeyValidator cacheKeyValidator(FrameworkCacheProperties properties) {
        return new CacheKeyValidator(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalCacheStore localCacheStore(FrameworkCacheProperties properties) {
        return new CaffeineLocalCacheStore(properties.getLocal());
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalCacheProvider localCacheProvider(LocalCacheStore localCacheStore, CacheKeyValidator validator) {
        return new LocalCacheProvider(localCacheStore, validator);
    }

    @Bean
    @ConditionalOnMissingBean
    public UnsupportedRedisCacheProvider redisCacheProvider() {
        return new UnsupportedRedisCacheProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public UnsupportedTieredCacheProvider tieredCacheProvider() {
        return new UnsupportedTieredCacheProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheTemplate cacheTemplate(LocalCacheProvider localCacheProvider,
            UnsupportedRedisCacheProvider redisCacheProvider, UnsupportedTieredCacheProvider tieredCacheProvider) {
        return new DefaultCacheTemplate(localCacheProvider, redisCacheProvider, tieredCacheProvider);
    }
}
