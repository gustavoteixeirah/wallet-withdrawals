package com.teixeirah.withdrawals.domain.wallet.withdraw;


import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletWithdrawCreatedEvent;

import java.math.BigDecimal;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class WalletWithdrawOperations {

    public static WalletWithdraw create(Long userId, BigDecimal amount, Recipient recipient) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive and higher than zero.");
        }

        requireNonNull(recipient, "Recipient cannot be null");
        requireNonNull(userId, "User ID cannot be null");

        final var id = UUID.randomUUID();
        final var walletWithdraw = new WalletWithdraw(id, userId, amount, recipient);
        walletWithdraw.registerDomainEvent(new WalletWithdrawCreatedEvent(walletWithdraw));
        return walletWithdraw;
    }
}
