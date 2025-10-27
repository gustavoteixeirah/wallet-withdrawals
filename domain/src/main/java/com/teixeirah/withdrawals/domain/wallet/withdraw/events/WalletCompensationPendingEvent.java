package com.teixeirah.withdrawals.domain.wallet.withdraw.events;

import com.teixeirah.withdrawals.domain.events.DomainEvent;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletCompensationPendingEvent(UUID withdrawalId, BigDecimal refundAmount) implements DomainEvent {
}