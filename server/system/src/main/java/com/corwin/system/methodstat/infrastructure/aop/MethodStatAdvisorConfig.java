package com.corwin.system.methodstat.infrastructure.aop;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AOP configuration that creates a pointcut advisor for method statistics collection.
 * @author Corwin 2026/4/1
 */
@Configuration
public class MethodStatAdvisorConfig {

    /**
     * Create the advisor that weaves statistics collection around pointcut-matched methods.
     * @param pointcutProvider the pointcut provider
     * @param interceptor the method interceptor
     * @return the configured advisor
     */
    @Bean
    public Advisor methodStatAdvisor(MethodStatPointcutProvider pointcutProvider,
            MethodStatCollectMethodInterceptor interceptor) {
        return new DefaultPointcutAdvisor(pointcutProvider.pointcut(), interceptor);
    }

}
