package com.teixeirah.withdrawals.infrastructure.support;

import java.util.HashMap;
import java.util.Map;

public final class WalletWithdrawalRequestBuilder {

    private final Map<String, Object> payload = new HashMap<>();

    private WalletWithdrawalRequestBuilder() {
        payload.put("userId", 1);
        payload.put("amount", 100.00);
        payload.put("recipientFirstName", "John");
        payload.put("recipientLastName", "Doe");
        payload.put("recipientRoutingNumber", "123456789");
        payload.put("recipientNationalId", "12345678901");
        payload.put("recipientAccountNumber", "987654321");
    }

    public static WalletWithdrawalRequestBuilder walletWithdrawalRequest() {
        return new WalletWithdrawalRequestBuilder();
    }

    public WalletWithdrawalRequestBuilder withAmount(double amount) {
        payload.put("amount", amount);
        return this;
    }

    public WalletWithdrawalRequestBuilder withRecipient(String firstName, String lastName) {
        payload.put("recipientFirstName", firstName);
        payload.put("recipientLastName", lastName);
        return this;
    }

    public WalletWithdrawalRequestBuilder withRecipientAccount(String accountNumber, String routingNumber) {
        payload.put("recipientAccountNumber", accountNumber);
        payload.put("recipientRoutingNumber", routingNumber);
        return this;
    }

    public WalletWithdrawalRequestBuilder withRecipientNationalId(String nationalId) {
        payload.put("recipientNationalId", nationalId);
        return this;
    }

    public WalletWithdrawalRequestBuilder withUserId(long userId) {
        payload.put("userId", userId);
        return this;
    }

    public Map<String, Object> build() {
        return new HashMap<>(payload);
    }
}
