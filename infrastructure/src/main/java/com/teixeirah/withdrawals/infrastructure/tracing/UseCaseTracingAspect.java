package com.teixeirah.withdrawals.infrastructure.tracing;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class UseCaseTracingAspect {

    private final OtelTracerFacade tracerFacade;

    public UseCaseTracingAspect(OtelTracerFacade tracerFacade) {
        this.tracerFacade = tracerFacade;
    }

    @Around("@annotation(com.teixeirah.withdrawals.application.usecase.annotations.TransactionalUseCase)")
    public Object traceUseCase(ProceedingJoinPoint joinPoint) throws Throwable {
        String spanName = "usecase." + joinPoint.getSignature().getName();
        return tracerFacade.newSpan(spanName, joinPoint::proceed);
    }
}
