package com.teixeirah.withdrawals.domain.payments;

public record PaymentSource(
        String type,
        PaymentSourceInformation sourceInformation,
        PaymentAccount account
) {
}