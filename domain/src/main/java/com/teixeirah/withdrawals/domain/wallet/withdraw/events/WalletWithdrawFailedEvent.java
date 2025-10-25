package com.teixeirah.withdrawals.domain.wallet.withdraw.events;

import com.teixeirah.withdrawals.domain.events.DomainEvent;

import java.util.Map;
import java.util.UUID;

public record WalletWithdrawFailedEvent(UUID withdrawalId, String reason, Map<String, String> metadata) implements DomainEvent {
    public WalletWithdrawFailedEvent(UUID withdrawalId, String reason) {
        this(withdrawalId, reason, Map.of());
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public WalletWithdrawFailedEvent withMetadata(Map<String, String> metadata) {
        return new WalletWithdrawFailedEvent(withdrawalId, reason, Map.copyOf(metadata));
    }
}