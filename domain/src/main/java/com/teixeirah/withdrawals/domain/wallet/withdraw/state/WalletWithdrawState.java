package com.teixeirah.withdrawals.domain.wallet.withdraw.state;

import com.teixeirah.withdrawals.domain.payments.PaymentProviderPort;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceProviderPort;
import com.teixeirah.withdrawals.domain.wallet.service.WalletBalancePort;
import com.teixeirah.withdrawals.domain.wallet.service.WalletServicePort;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;

public sealed interface WalletWithdrawState permits PendingDebitState, WalletDebitedState, CompletedState, FailedState {

    default void processDebit(WalletWithdraw context, WalletBalancePort balancePort, WalletServicePort walletServicePort) {
        throw new IllegalStateException("Cannot process debit in state " + this.getClass().getSimpleName());
    }

    default void processPayment(WalletWithdraw context, PaymentProviderPort paymentProviderPort, PaymentSourceProviderPort paymentSourceProviderPort) {
        throw new IllegalStateException("Cannot process payment in state " + this.getClass().getSimpleName());
    }

}
