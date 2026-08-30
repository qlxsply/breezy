package com.corwin.framework.web.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Configuration for CORS and other servlet-level filters.
 *
 * <p>Configures public bearer-token CORS without allowing cross-origin cookies.
 *
 * @author Corwin 2025/10/11
 */
@Configuration
public class FilterConfiguration {

  @Bean
  public FilterRegistrationBean<CorsFilter> corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(false);
    config.addAllowedOriginPattern("*");
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");
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
