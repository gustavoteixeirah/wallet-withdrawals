package com.teixeirah.withdrawals.infrastructure.aop;

import com.teixeirah.withdrawals.application.usecase.annotations.TransactionalUseCase;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Aspect
@Component
public class TransactionalAspect {
    private final TransactionTemplate transactionTemplate;

    public TransactionalAspect(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

    }

    @Around("@within(com.teixeirah.withdrawals.application.usecase.annotations.TransactionalUseCase) || @annotation(com.teixeirah.withdrawals.application.usecase.annotations.TransactionalUseCase)")
    public Object applyTransaction(ProceedingJoinPoint joinPoint) {
        var signature = (MethodSignature) joinPoint.getSignature();
        var ann = signature.getMethod().getAnnotation(TransactionalUseCase.class);
        // Fallback to class-level if method-level not found
        if (ann == null) {
            ann = joinPoint.getTarget().getClass().getAnnotation(TransactionalUseCase.class);
        }

        final var finalAnn = ann;
        return transactionTemplate.execute(status -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                if (shouldRollback(e, finalAnn)) {
                    status.setRollbackOnly();
                }
                throw new RuntimeException(e);  // Rethrow wrapped
            }
        });
    }

    private boolean shouldRollback(Throwable e, TransactionalUseCase ann) {
        // Check noRollbackFor first
        for (Class<?> noRollbackClass : ann.noRollbackFor()) {
            if (noRollbackClass.isAssignableFrom(e.getClass())) {
                return false;
            }
        }
        // Check rollbackFor or default
        for (Class<?> rollbackClass : ann.rollbackFor()) {
            if (rollbackClass.isAssignableFrom(e.getClass())) {
                return true;
            }
        }
        // Default: rollback on RuntimeException or Error if no explicit match
        return RuntimeException.class.isAssignableFrom(e.getClass()) || Error.class.isAssignableFrom(e.getClass());
    }
}