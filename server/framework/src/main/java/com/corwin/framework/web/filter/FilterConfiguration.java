package com.corwin.framework.web.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Configuration for CORS and other servlet-level filters.
 * <p>
 * Configures a global CORS filter that allows all origins, headers, and
 * methods, with support for credentials and common exposed headers.
 *
 * @author Corwin 2025/10/11
 */
@Configuration
public class FilterConfiguration {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);    // allow cookies / credentials
        config.addAllowedOriginPattern("*"); // allow all origins
        config.addAllowedHeader("*");        // allow all headers
        config.addAllowedMethod("*");        // allow all HTTP methods
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
