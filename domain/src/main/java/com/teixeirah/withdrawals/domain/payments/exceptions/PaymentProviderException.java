package com.teixeirah.withdrawals.domain.payments.exceptions;

public class PaymentProviderException extends RuntimeException {
    public PaymentProviderException(String message) {
        super(message);
    }
}
