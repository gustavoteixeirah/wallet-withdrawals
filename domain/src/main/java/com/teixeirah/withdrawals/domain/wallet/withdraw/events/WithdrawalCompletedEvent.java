package com.teixeirah.withdrawals.domain.wallet.withdraw.events;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;

import java.util.Map;
import java.util.UUID;

public record WithdrawalCompletedEvent(UUID withdrawalId, Map<String, String> metadata) implements DomainEvent {

    public WithdrawalCompletedEvent(UUID withdrawalId) {
        this(withdrawalId, Map.of());
    }

    public WithdrawalCompletedEvent(WalletWithdraw withdrawal) {
        this(withdrawal.getId(), Map.of());
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public WithdrawalCompletedEvent withMetadata(Map<String, String> metadata) {
        return new WithdrawalCompletedEvent(withdrawalId, Map.copyOf(metadata));
    }
}