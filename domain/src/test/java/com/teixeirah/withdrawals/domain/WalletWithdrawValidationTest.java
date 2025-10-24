package com.teixeirah.withdrawals.domain;

import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.value.objects.Account;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawOperations;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class WalletWithdrawValidationTest {

    @Test
    void shouldFailToCreateTransactionWithZeroAmount() {
        WalletWithdrawOperations operations = new WalletWithdrawOperations();
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        assertThrows(IllegalArgumentException.class, () -> operations.create(1L, BigDecimal.ZERO, recipient));
    }

    @Test
    void shouldFailToCreateTransactionWithNegativeAmount() {
        WalletWithdrawOperations operations = new WalletWithdrawOperations();
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        assertThrows(IllegalArgumentException.class, () -> operations.create(1L, BigDecimal.valueOf(-50.00), recipient));
    }

    @Test
    void shouldFailToCreateTransactionWithNullAmount() {
        WalletWithdrawOperations operations = new WalletWithdrawOperations();
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        assertThrows(IllegalArgumentException.class, () -> operations.create(1L, null, recipient));
    }

    @Test
    void shouldFailToCreateTransactionWithNullRecipient() {
        WalletWithdrawOperations operations = new WalletWithdrawOperations();
        assertThrows(NullPointerException.class, () -> operations.create(1L, BigDecimal.valueOf(100.00), null));
    }

    @Test
    void shouldFailToCreateTransactionWithNullUserId() {
        WalletWithdrawOperations operations = new WalletWithdrawOperations();
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        assertThrows(NullPointerException.class, () -> operations.create(null, BigDecimal.valueOf(100.00), recipient));
    }
}
