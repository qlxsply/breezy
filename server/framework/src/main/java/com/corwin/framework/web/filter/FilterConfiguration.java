package com.corwin.framework.web.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * @author Corwin 2025/10/11
 */
@Configuration
public class FilterConfiguration {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);    // 支持 Cookie
        config.addAllowedOriginPattern("*"); // 允许所有域名
        config.addAllowedHeader("*");        // 所有请求头
        config.addAllowedMethod("*");        // 所有方法
        config.addExposedHeader("Content-Disposition");
        config.addExposedHeader("Content-Type");
        config.addExposedHeader("Content-Length");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

}
