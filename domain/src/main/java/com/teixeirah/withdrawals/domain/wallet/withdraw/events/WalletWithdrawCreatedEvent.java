package com.teixeirah.withdrawals.domain.wallet.withdraw.events;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;

import java.util.Map;

public record WalletWithdrawCreatedEvent(WalletWithdraw walletWithdraw, Map<String, String> metadata) implements DomainEvent {
    public WalletWithdrawCreatedEvent(WalletWithdraw walletWithdraw) {
        this(walletWithdraw, Map.of());
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public WalletWithdrawCreatedEvent withMetadata(Map<String, String> metadata) {
        return new WalletWithdrawCreatedEvent(walletWithdraw, Map.copyOf(metadata));
    }
}
