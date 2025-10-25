package com.teixeirah.withdrawals.domain;

import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawOperations;
import com.teixeirah.withdrawals.domain.value.objects.Account;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TransactionValidationTest {

    @Test
    void shouldFailToCreateTransactionWithZeroAmount() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        assertThrows(IllegalArgumentException.class, () -> WalletWithdrawOperations.create(1L, BigDecimal.ZERO, recipient));
    }

    @Test
    void shouldFailToCreateTransactionWithNegativeAmount() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        assertThrows(IllegalArgumentException.class, () -> WalletWithdrawOperations.create(1L, BigDecimal.valueOf(-50.00), recipient));
    }

    @Test
    void shouldFailToCreateTransactionWithNullAmount() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        assertThrows(IllegalArgumentException.class, () -> WalletWithdrawOperations.create(1L, null, recipient));
    }

    @Test
    void shouldFailToCreateTransactionWithNullRecipient() {
        assertThrows(NullPointerException.class, () -> WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), null));
    }

    @Test
    void shouldFailToCreateTransactionWithNullUserId() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        assertThrows(NullPointerException.class, () -> WalletWithdrawOperations.create(null, BigDecimal.valueOf(100.00), recipient));
    }
}
