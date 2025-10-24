package com.teixeirah.withdrawals.domain.wallet.withdraw.exceptions;

public class WalletWithdrawNotFoundException extends RuntimeException {
    public WalletWithdrawNotFoundException(String message) {
        super(message);
    }
}
