package com.teixeirah.withdrawals.domain.payments;

import java.math.BigDecimal;

public record PaymentRequest(
        PaymentSource source,
        PaymentDestination destination,
        BigDecimal amount
) {
}