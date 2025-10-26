package com.teixeirah.withdrawals.domain.wallet.withdraw.state;

import com.teixeirah.withdrawals.domain.wallet.service.WalletBalancePort;
import com.teixeirah.withdrawals.domain.wallet.service.WalletServicePort;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.InsufficientFundsException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletNotFoundException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletServiceException;
import com.teixeirah.withdrawals.domain.wallet.withdraw.FeeCalculator;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletDebitedEvent;

public final class PendingDebitState implements WalletWithdrawState {

    @Override
    public void processDebit(WalletWithdraw walletWithdraw, WalletBalancePort balancePort, WalletServicePort walletServicePort) {
        try {
            final var totalToDebit = FeeCalculator.calculateTotalToDebit(walletWithdraw);

            final var currentBalance = balancePort.getBalance(walletWithdraw.getUserId());
            if (currentBalance.compareTo(totalToDebit) < 0) {
                walletWithdraw.markAsFailed(new FailedState(), "Insufficient funds");
                return;
            }

            final var walletTransactionId = walletServicePort.debit(walletWithdraw.getUserId(), totalToDebit, walletWithdraw.getId());
            walletWithdraw.setWalletTransactionIdRef(String.valueOf(walletTransactionId));
            walletWithdraw.changeState(new WalletDebitedState(), WalletWithdrawStatus.WALLET_DEBITED);
            walletWithdraw.registerDomainEvent(new WalletDebitedEvent(walletWithdraw));
        } catch (InsufficientFundsException e) {
            walletWithdraw.markAsFailed(new FailedState(), "Insufficient funds");
        } catch (WalletNotFoundException e) {
            walletWithdraw.markAsFailed(new FailedState(), "Wallet not found");
        } catch (WalletServiceException e) {
            walletWithdraw.markAsFailed(new FailedState(), "Wallet service error: " + e.getMessage());
        }
    }
}
