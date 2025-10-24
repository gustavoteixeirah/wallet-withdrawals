package com.teixeirah.withdrawals.domain.wallet.withdraw.events;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;

import java.util.UUID;

public record WithdrawalCompletedEvent(UUID withdrawalId) implements DomainEvent {

    public WithdrawalCompletedEvent(WalletWithdraw withdrawal) {
        this(withdrawal.getId());
    }

}