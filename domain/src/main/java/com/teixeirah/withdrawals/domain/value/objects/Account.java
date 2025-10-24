package com.teixeirah.withdrawals.domain.value.objects;

public record Account(String accountNumber, String routingNumber) {

    public Account {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required.");
        }
        if (routingNumber == null || routingNumber.isBlank()) {
            throw new IllegalArgumentException("Account routing number is required.");
        }
    }
}
