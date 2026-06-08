package com.corwin.system.methodstat.infrastructure.aop;

import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.stereotype.Component;

/**
 * @author Corwin 2026/4/1
 */
@Component
public class MethodStatPointcutProvider {

    private static final String EXPRESSION = "(execution(public * *(..)) && @annotation(com.corwin.system.methodstat.domain.model.MethodStat))" + " || execution(public * com.corwin.*.interfaces.web.*.*(..))";

    private final AspectJExpressionPointcut pointcut;

    public MethodStatPointcutProvider() {
        this.pointcut = new AspectJExpressionPointcut();
        this.pointcut.setExpression(EXPRESSION);
    }

    public Pointcut pointcut() {
        return pointcut;
    }

}
