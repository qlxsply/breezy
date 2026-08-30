package com.corwin.framework.web.sort;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration for the sort-rule infrastructure.
 *
 * <p>Registers a default {@link SortRuleProvider}, the {@link SortRuleInterceptor}, and its
 * corresponding {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurer} to plug
 * the interceptor into the MVC lifecycle.
 *
 * @author Corwin 2026/7/29
 */
@AutoConfiguration
public class SortRuleAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public SortRuleProvider sortRuleProvider() {
    return new DefaultSortRuleProvider();
  }

  @Bean
  public SortRuleInterceptor sortRuleInterceptor(SortRuleProvider provider) {
    return new SortRuleInterceptor(provider);
  }

  @Bean
  public WebMvcConfigurer sortRuleWebMvcConfigurer(SortRuleInterceptor interceptor) {
    return new WebMvcConfigurer() {
      @Override
      public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor);
      }
    };
  }
}
