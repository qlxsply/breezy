package com.corwin.framework.json;

import java.util.TimeZone;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson auto-configuration that registers {@link CustomModule} and sets the default time zone.
 *
 * @author Corwin 2026/1/7
 */
@Configuration
public class JacksonConfiguration {

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer timeModuleCustomizer() {
    return builder -> builder.modules(new CustomModule()).timeZone(TimeZone.getDefault());
  }
}
