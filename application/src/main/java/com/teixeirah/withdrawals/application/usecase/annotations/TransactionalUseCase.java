package com.teixeirah.withdrawals.application.usecase.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TransactionalUseCase {
    Class<? extends Throwable>[] rollbackFor() default {RuntimeException.class, Error.class};
    Class<? extends Throwable>[] noRollbackFor() default {};
}