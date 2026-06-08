package com.corwin.framework.json;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
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
