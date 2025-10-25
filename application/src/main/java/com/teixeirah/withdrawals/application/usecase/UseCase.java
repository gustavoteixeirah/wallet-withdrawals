package com.teixeirah.withdrawals.application.usecase;

public interface UseCase<I, O> {
    O execute(I input);
}