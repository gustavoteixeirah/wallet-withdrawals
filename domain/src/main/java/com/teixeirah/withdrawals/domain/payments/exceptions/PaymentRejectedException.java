package com.teixeirah.withdrawals.domain.payments.exceptions;

public class PaymentRejectedException extends RuntimeException {
    public PaymentRejectedException(String message) {
        super(message);
    }
}
