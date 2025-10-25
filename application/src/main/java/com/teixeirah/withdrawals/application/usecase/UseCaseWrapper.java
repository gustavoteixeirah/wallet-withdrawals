package com.teixeirah.withdrawals.application.usecase;

public interface UseCaseWrapper {
    <T extends UseCase<?, ?>> T wrap(T useCase, WrapperContext context);
}