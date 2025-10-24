package com.teixeirah.withdrawals.domain.wallet.withdraw.events;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;

public record WalletWithdrawCreatedEvent(WalletWithdraw walletWithdraw) implements DomainEvent {
}
