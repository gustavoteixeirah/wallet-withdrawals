package com.teixeirah.withdrawals.domain.payments;

public record PaymentDestination(
        String name,
        PaymentAccount account
) {
}