package com.corwin.system.methodstat.infrastructure.aop;

import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.stereotype.Component;

/**
 * Provides the AOP pointcut definition that matches methods annotated with {@code @MethodStat}
 * or all public methods in web controllers ({@code com.corwin..interfaces.web..*}).
 * @author Corwin 2026/4/1
 */
@Component
public class MethodStatPointcutProvider {

    private static final String METHOD_STAT_ANNOTATION = "com.corwin.system.methodstat.domain.model.MethodStat";
    private static final String EXPRESSION = "(execution(public * *(..)) && (@annotation(" + METHOD_STAT_ANNOTATION
            + ") || @within(" + METHOD_STAT_ANNOTATION + ")))"
            + " || execution(public * com.corwin..interfaces.web..*(..))";

    private final AspectJExpressionPointcut pointcut;

    public MethodStatPointcutProvider() {
        this.pointcut = new AspectJExpressionPointcut();
        this.pointcut.setExpression(EXPRESSION);
    }

    /**
     * @return the configured AOP pointcut
     */
    public Pointcut pointcut() {
        return pointcut;
    }

}
