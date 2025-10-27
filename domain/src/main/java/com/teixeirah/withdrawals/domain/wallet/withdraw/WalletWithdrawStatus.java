package com.teixeirah.withdrawals.domain.wallet.withdraw;

public enum WalletWithdrawStatus {
    CREATED,
    WALLET_DEBITED,
    COMPLETED,
    FAILED,
    COMPENSATION_PENDING,
    REFUNDED
}
