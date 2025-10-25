package com.teixeirah.withdrawals.domain.wallet.withdraw.events;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;

import java.util.Map;
import java.util.UUID;

public record WalletDebitedEvent(UUID withdrawalId, Map<String, String> metadata) implements DomainEvent {

    public WalletDebitedEvent(UUID withdrawalId) {
        this(withdrawalId, Map.of());
    }

    public WalletDebitedEvent(WalletWithdraw withdrawal) {
        this(withdrawal.getId(), Map.of());
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public WalletDebitedEvent withMetadata(Map<String, String> metadata) {
        return new WalletDebitedEvent(withdrawalId, Map.copyOf(metadata));
    }
}