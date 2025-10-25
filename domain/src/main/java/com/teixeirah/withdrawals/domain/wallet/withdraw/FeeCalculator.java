package com.teixeirah.withdrawals.domain.wallet.withdraw;

import java.math.BigDecimal;

public class FeeCalculator {

    public static final BigDecimal FEE = BigDecimal.valueOf(0.10);

    public static BigDecimal calculateTotalToDebit(WalletWithdraw walletWithdraw) {
        return walletWithdraw.getAmount().add(walletWithdraw.getFee());
    }
}