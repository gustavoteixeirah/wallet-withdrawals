package com.teixeirah.withdrawals.domain.wallet.withdraw;

import com.teixeirah.withdrawals.domain.events.DomainEvent;

public record WalletWithdrawCreatedEvent(WalletWithdraw walletWithdraw) implements DomainEvent {
}
