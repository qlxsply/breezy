package com.corwin.system.methodstat.infrastructure.aop;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Corwin 2026/4/1
 */
@Configuration
public class MethodStatAdvisorConfig {

    @Bean
    public Advisor methodStatAdvisor(MethodStatPointcutProvider pointcutProvider,
            MethodStatCollectMethodInterceptor interceptor) {
        return new DefaultPointcutAdvisor(pointcutProvider.pointcut(), interceptor);
    }

}
