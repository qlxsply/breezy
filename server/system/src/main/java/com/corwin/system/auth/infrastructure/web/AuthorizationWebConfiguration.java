package com.corwin.system.auth.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration that registers the {@link AuthorizationInterceptor}
 * to intercept all {@code /api/**} requests.
 *
 * @author Corwin 2026/4/19
 */
@Configuration
@RequiredArgsConstructor
public class AuthorizationWebConfiguration implements WebMvcConfigurer {

    private final AuthorizationInterceptor authorizationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register the authorization interceptor for all API paths
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/api/**");
    }
}
