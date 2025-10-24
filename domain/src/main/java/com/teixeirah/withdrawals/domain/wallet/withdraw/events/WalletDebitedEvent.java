package com.teixeirah.withdrawals.domain.wallet.withdraw.events;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;

import java.util.UUID;

public record WalletDebitedEvent(UUID withdrawalId) implements DomainEvent {

    public WalletDebitedEvent(WalletWithdraw withdrawal) {
        this(withdrawal.getId());
    }

}