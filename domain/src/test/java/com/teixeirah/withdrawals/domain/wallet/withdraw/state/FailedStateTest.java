package com.teixeirah.withdrawals.domain.wallet.withdraw.state;

import com.teixeirah.withdrawals.domain.value.objects.Account;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawOperations;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FailedStateTest {

    private FailedState failedState;
    private WalletWithdraw walletWithdraw;

    @BeforeEach
    void setUp() {
        failedState = new FailedState();

        // Create a real WalletWithdraw instance and put it in FAILED state
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        walletWithdraw = WalletWithdrawOperations.create(1L, new BigDecimal("100.00"), recipient);
        walletWithdraw.changeState(new FailedState(), WalletWithdrawStatus.FAILED);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenProcessDebitIsCalled() {
        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> failedState.processDebit(walletWithdraw, null)
        );

        assertEquals("Cannot process debit in state FailedState", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenProcessPaymentIsCalled() {
        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> failedState.processPayment(walletWithdraw, null, null)
        );

        assertEquals("Cannot process payment in state FailedState", exception.getMessage());
    }
}