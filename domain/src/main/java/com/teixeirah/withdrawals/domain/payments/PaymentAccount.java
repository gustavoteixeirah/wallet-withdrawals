package com.teixeirah.withdrawals.domain.payments;

public record PaymentAccount(
        String accountNumber,
        String currency,
        String routingNumber
) {
}