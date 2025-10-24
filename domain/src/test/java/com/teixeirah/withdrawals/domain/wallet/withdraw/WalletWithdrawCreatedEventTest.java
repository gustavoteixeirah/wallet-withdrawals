package com.teixeirah.withdrawals.domain.wallet.withdraw;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.value.objects.Account;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class WalletWithdrawCreatedEventTest {

    @Test
    void shouldCreateTransactionCreatedEventWithTransaction() {
        WalletWithdrawOperations operations = new WalletWithdrawOperations();
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw walletWithdraw = operations.create(1L, BigDecimal.valueOf(100.00), recipient);

        WalletWithdrawCreatedEvent event = new WalletWithdrawCreatedEvent(walletWithdraw);

        assertNotNull(event);
        assertEquals(walletWithdraw, event.walletWithdraw());
        assertInstanceOf(DomainEvent.class, event);
    }
}
