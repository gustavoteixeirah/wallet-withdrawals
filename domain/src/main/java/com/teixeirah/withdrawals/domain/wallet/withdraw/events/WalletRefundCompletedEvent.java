package com.teixeirah.withdrawals.domain.wallet.withdraw.events;

import com.teixeirah.withdrawals.domain.events.DomainEvent;

import java.util.UUID;

public record WalletRefundCompletedEvent(UUID withdrawalId) implements DomainEvent {
}