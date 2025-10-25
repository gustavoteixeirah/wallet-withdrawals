package com.teixeirah.withdrawals.infrastructure.wrappers;

import com.teixeirah.withdrawals.application.usecase.UseCase;
import com.teixeirah.withdrawals.application.usecase.UseCaseWrapper;
import com.teixeirah.withdrawals.application.usecase.WrapperContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class TransactionalUseCaseWrapper implements UseCaseWrapper {

    private final TransactionTemplate transactionTemplate;

    public TransactionalUseCaseWrapper(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    @Override
    public <T extends UseCase<?, ?>> T wrap(T useCase, WrapperContext context) {
        Class<?>[] interfaces = useCase.getClass().getInterfaces();
        return (T) Proxy.newProxyInstance(
            useCase.getClass().getClassLoader(),
            interfaces,
            new TransactionalInvocationHandler(useCase, transactionTemplate)
        );
    }

    private static class TransactionalInvocationHandler implements InvocationHandler {
        private final UseCase<?, ?> delegate;
        private final TransactionTemplate transactionTemplate;

        public TransactionalInvocationHandler(UseCase<?, ?> delegate, TransactionTemplate transactionTemplate) {
            this.delegate = delegate;
            this.transactionTemplate = transactionTemplate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("execute") && args.length == 1) {
                return transactionTemplate.execute(status -> {
                    try {
                        return method.invoke(delegate, args);
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        throw new RuntimeException(e);
                    }
                });
            }
            return method.invoke(delegate, args);
        }
    }
}