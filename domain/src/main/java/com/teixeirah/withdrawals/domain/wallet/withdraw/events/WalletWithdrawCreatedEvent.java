package com.teixeirah.withdrawals.domain.wallet.withdraw.events;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;

import java.util.UUID;

public record WalletWithdrawCreatedEvent(UUID withdrawalId) implements DomainEvent {

    public WalletWithdrawCreatedEvent(WalletWithdraw walletWithdraw) {
        this(walletWithdraw.getId());
    }
}
