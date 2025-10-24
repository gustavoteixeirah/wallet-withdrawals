package com.teixeirah.withdrawals.application.command;

import java.util.UUID;

public record ProcessWalletDebitCommand(UUID walletWithdrawId) {
    public ProcessWalletDebitCommand {
        if (walletWithdrawId == null) {
            throw new IllegalArgumentException("WalletWithdraw ID cannot be null.");
        }
    }
}